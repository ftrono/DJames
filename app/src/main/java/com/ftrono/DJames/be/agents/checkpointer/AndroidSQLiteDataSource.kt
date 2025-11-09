package com.ftrono.DJames.be.agents.checkpointer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource


/**
 * Provides a JDBC DataSource wrapper for the app-local SQLite database.
 */
class AndroidSQLiteDataSource(context: Context, dbName: String = "langgraph4j.db") : DataSource {

    private val dbFile: File = File(context.cacheDir, dbName)   // Cache dir
    // private val dbFile = File(context.filesDir, dbName)   // App's files dir
    private val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

    init {
        // Load SQLite JDBC driver (org.sqlite.JDBC)
        Class.forName("org.sqlite.JDBC")
    }

    override fun getConnection(): Connection {
        return DriverManager.getConnection(dbUrl)
    }

    override fun getConnection(username: String?, password: String?): Connection {
        return getConnection()
    }

    override fun getLogWriter() = null
    override fun setLogWriter(out: java.io.PrintWriter?) {}
    override fun setLoginTimeout(seconds: Int) {}
    override fun getLoginTimeout() = 0
    override fun getParentLogger(): java.util.logging.Logger? = null
    override fun <T> unwrap(iface: Class<T>?): T? = null
    override fun isWrapperFor(iface: Class<*>?): Boolean = false
}
