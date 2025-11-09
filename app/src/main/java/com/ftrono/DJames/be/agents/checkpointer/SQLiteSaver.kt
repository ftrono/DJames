package com.ftrono.DJames.be.agents.checkpointer

import android.content.Context
import org.bsc.langgraph4j.RunnableConfig
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver
import org.bsc.langgraph4j.checkpoint.Checkpoint
import org.bsc.langgraph4j.checkpoint.MemorySaver
import tools.jackson.databind.ObjectMapper
import java.sql.*
import java.util.*
import javax.sql.DataSource

/**
 * <p>
 * SqliteSaver is an extension of MemorySaver that enables persistent,
 * reliable storage of workflow state in a SQLite database.
 * </p>
 * <p>
 * Two tables are used to store the workflow state:
 *
 * <pre>
 *     CREATE TABLE LANGRAPH4J_THREAD (
 *          thread_id TEXT PRIMARY KEY,
 *          thread_name TEXT,
 *          is_released INTEGER DEFAULT 0 NOT NULL
 *     );
 *     CREATE UNIQUE INDEX IF NOT EXISTS IDX_LANGRAPH4J_THREAD_NAME_RELEASED
 *          ON LANGRAPH4J_THREAD(thread_name, is_released);
 *
 *     CREATE TABLE LANGRAPH4J_CHECKPOINT (
 *          checkpoint_id TEXT PRIMARY KEY,
 *          thread_id TEXT NOT NULL,
 *          node_id TEXT,
 *          next_node_id TEXT,
 *          state_data TEXT NOT NULL,
 *          saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *
 *          FOREIGN KEY(thread_id)
 *              REFERENCES LANGRAPH4J_THREAD(thread_id)
 *              ON DELETE CASCADE
 *     );
 * </pre>
 * </p>
 */
class SQLiteSaver private constructor(
    private val dataSource: DataSource,
    private val createOption: CreateOption
) : MemorySaver() {

    private val objectMapper = ObjectMapper()

    // DDL statements
    private val CREATE_THREAD_TABLE = """
        CREATE TABLE IF NOT EXISTS LANGRAPH4J_THREAD (
           thread_id TEXT PRIMARY KEY,
           thread_name TEXT,
           is_released INTEGER DEFAULT 0 NOT NULL
        );
    """.trimIndent()

    private val INDEX_THREAD_TABLE = """
        CREATE UNIQUE INDEX IF NOT EXISTS IDX_LANGRAPH4J_THREAD_NAME_RELEASED
          ON LANGRAPH4J_THREAD(thread_name, is_released);
    """.trimIndent()

    private val CREATE_CHECKPOINT_TABLE = """
        CREATE TABLE IF NOT EXISTS LANGRAPH4J_CHECKPOINT (
           checkpoint_id TEXT PRIMARY KEY,
           thread_id TEXT NOT NULL,
           node_id TEXT,
           next_node_id TEXT,
           state_data TEXT NOT NULL,
           saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
           FOREIGN KEY(thread_id)
               REFERENCES LANGRAPH4J_THREAD(thread_id)
               ON DELETE CASCADE
        );
    """.trimIndent()

    private val DROP_CHECKPOINT_TABLE = "DROP TABLE IF EXISTS LANGRAPH4J_CHECKPOINT"
    private val DROP_THREAD_TABLE = "DROP TABLE IF EXISTS LANGRAPH4J_THREAD"

    // DML statements — adapted for SQLite syntax
    private val UPSERT_THREAD = """
        INSERT INTO LANGRAPH4J_THREAD (thread_id, thread_name, is_released)
        VALUES (?, ?, 0)
        ON CONFLICT(thread_name, is_released) DO NOTHING;
    """.trimIndent()

    private val INSERT_CHECKPOINT = """
        INSERT INTO LANGRAPH4J_CHECKPOINT(checkpoint_id, thread_id, node_id, next_node_id, state_data)
        SELECT ?, thread_id, ?, ?, ?
        FROM LANGRAPH4J_THREAD
        WHERE thread_name = ? AND is_released = 0;
    """.trimIndent()

    private val UPDATE_CHECKPOINT = """
        UPDATE LANGRAPH4J_CHECKPOINT
        SET
          checkpoint_id = ?,
          node_id = ?,
          next_node_id = ?,
          state_data = ?
        WHERE checkpoint_id = ?;
    """.trimIndent()

    private val SELECT_CHECKPOINTS = """
        SELECT
          c.checkpoint_id,
          c.node_id,
          c.next_node_id,
          c.state_data
        FROM LANGRAPH4J_CHECKPOINT c
          INNER JOIN LANGRAPH4J_THREAD t ON c.thread_id = t.thread_id
        WHERE t.thread_name = ? AND t.is_released != 1
        ORDER BY c.saved_at DESC;
    """.trimIndent()

    private val DELETE_CHECKPOINTS = "DELETE FROM LANGRAPH4J_CHECKPOINT WHERE checkpoint_id = ?"
    private val RELEASE_THREAD = """
        UPDATE LANGRAPH4J_THREAD SET is_released = 1
        WHERE thread_name = ? AND is_released = 0;
    """.trimIndent()

    init {
        initTables()
    }

    override fun loadedCheckpoints(
        config: RunnableConfig,
        checkpoints: LinkedList<Checkpoint>
    ): LinkedList<Checkpoint> {
        if (checkpoints.isNotEmpty()) return checkpoints

        val threadName = config.threadId().orElse(THREAD_ID_DEFAULT)
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(SELECT_CHECKPOINTS).use { ps ->
                    ps.setString(1, threadName)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val jsonString = rs.getString(4)
                            val state: Map<String, Any> =
                                objectMapper.readValue(jsonString, Map::class.java) as Map<String, Any>
                            val checkpoint = Checkpoint.builder()
                                .id(rs.getString(1))
                                .nodeId(rs.getString(2))
                                .nextNodeId(rs.getString(3))
                                .state(state)
                                .build()
                            checkpoints.add(checkpoint)
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            throw Exception("Unable to load checkpoints", e)
        }
        return checkpoints
    }

    override fun insertedCheckpoint(
        config: RunnableConfig,
        checkpoints: LinkedList<Checkpoint>,
        checkpoint: Checkpoint
    ) {
        val threadName = config.threadId().orElse(THREAD_ID_DEFAULT)
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(UPSERT_THREAD).use { upsertStmt ->
                    upsertStmt.setString(1, UUID.randomUUID().toString())
                    upsertStmt.setString(2, threadName)
                    upsertStmt.execute()
                }

                connection.prepareStatement(INSERT_CHECKPOINT).use { insertStmt ->
                    insertStmt.setString(1, checkpoint.id)
                    insertStmt.setString(2, checkpoint.nodeId)
                    insertStmt.setString(3, checkpoint.nextNodeId)
                    insertStmt.setString(4, objectMapper.writeValueAsString(checkpoint.state))
                    insertStmt.setString(5, threadName)
                    insertStmt.execute()
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Unable to insert checkpoint", e)
        }
    }

    override fun releasedCheckpoints(
        config: RunnableConfig,
        checkpoints: LinkedList<Checkpoint>,
        releaseTag: BaseCheckpointSaver.Tag
    ) {
        val threadName = config.threadId().orElse(THREAD_ID_DEFAULT)
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(RELEASE_THREAD).use { ps ->
                    ps.setString(1, threadName)
                    ps.execute()
                }
            }
        } catch (e: SQLException) {
            throw Exception("Unable to release checkpoint", e)
        }
    }

    override fun updatedCheckpoint(
        config: RunnableConfig,
        checkpoints: LinkedList<Checkpoint>,
        checkpoint: Checkpoint
    ) {
        if (config.checkPointId().isPresent) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement(UPDATE_CHECKPOINT).use { ps ->
                        ps.setString(1, checkpoint.id)
                        ps.setString(2, checkpoint.nodeId)
                        ps.setString(3, checkpoint.nextNodeId)
                        ps.setString(4, objectMapper.writeValueAsString(checkpoint.state))
                        ps.setString(5, config.checkPointId().get())
                        ps.execute()
                    }
                }
            } catch (e: SQLException) {
                throw Exception("Unable to update checkpoint", e)
            }
        } else {
            insertedCheckpoint(config, checkpoints, checkpoint)
        }
    }

    protected fun initTables() {
        try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    if (createOption == CreateOption.CREATE_OR_REPLACE) {
                        statement.addBatch(DROP_CHECKPOINT_TABLE)
                        statement.addBatch(DROP_THREAD_TABLE)
                        statement.executeBatch()
                    }
                    if (createOption == CreateOption.CREATE_OR_REPLACE ||
                        createOption == CreateOption.CREATE_IF_NOT_EXISTS
                    ) {
                        statement.execute(CREATE_THREAD_TABLE)
                        statement.execute(CREATE_CHECKPOINT_TABLE)
                        statement.execute(INDEX_THREAD_TABLE)
                    }
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Unable to create tables", e)
        }
    }

    class Builder {
        private var dataSource: DataSource? = null
        private var createOption: CreateOption = CreateOption.CREATE_IF_NOT_EXISTS

        fun dataSource(dataSource: DataSource) = apply { this.dataSource = dataSource }
        fun createOption(createOption: CreateOption) = apply { this.createOption = createOption }

        fun build(): SQLiteSaver {
            requireNotNull(dataSource) { "DataSource must be set" }
            return SQLiteSaver(dataSource!!, createOption)
        }
    }

    fun createSqliteSaver(context: Context): SQLiteSaver {
        val dataSource = AndroidSQLiteDataSource(context)
        return builder()
            .createOption(CreateOption.CREATE_IF_NOT_EXISTS)
            .dataSource(dataSource)
            .build()
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
