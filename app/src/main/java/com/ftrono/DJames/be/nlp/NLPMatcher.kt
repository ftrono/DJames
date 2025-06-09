package com.ftrono.DJames.be.nlp

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.midThreshold
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.math.roundToInt

class NLPMatcher (private val context: Context) {
    private val TAG = NLPMatcher::class.java.simpleName

    //Match item from user query against user vocabulary:
    fun matchVocabulary(filter: String, text: String, threshold: Int = midThreshold): String {
        var matchId = ""
        val vocMap = libUtils.getAliasesMap(filter)
        if (text != "" && vocMap.isNotEmpty()) {
            //Init:
            var score = 0
            val listEvalued = text.split(", ")
            val listConfirmed = mutableListOf<String>()
            val scoresMap = mutableMapOf<String, Int>()

            //Check each evaluated item:
            for (eval in listEvalued) {
                //Check each artist id:
                for (curId in vocMap.keys) {
                    val aliasScores = mutableListOf<Int>()
                    //Check each alias:
                    for (curAlias in vocMap[curId]!!) {
                        if (filter == "playlist") {
                            val namePartial = FuzzySearch.partialRatio(curAlias, eval.lowercase())
                            val nameFull = FuzzySearch.ratio(curAlias, eval.lowercase())
                            score = listOf<Int>(namePartial, nameFull).average().roundToInt()
                        } else {
                            score = FuzzySearch.ratio(curAlias, eval.lowercase())
                        }
                        Log.d(TAG, "VOC CONFIRMATION: COMPARING $curAlias WITH ${eval.lowercase()}, MATCH: $score")
                        aliasScores.add(score)
                    }
                    //Get Max alias score and add globally only if high enough:
                    val maxScore = aliasScores.max()
                    if (!scoresMap.keys.contains(curId) && maxScore >= threshold) {
                        scoresMap[curId] = maxScore
                    }
                }
                if (scoresMap.isNotEmpty()) {
                    //Sort and get highest match:
                    val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
                    Log.d(TAG, "SORTED MAP FOR $eval: $sortedScores")
                    listConfirmed.add(sortedScores.keys.toList()[0])
                    lastLog.keyInfo.vocScore = sortedScores.values.toList()[0]
                }
            }
            //Final:
            if (listConfirmed.isNotEmpty()) {
                Log.d(TAG, "listConfirmed: $listConfirmed")
                matchId = listConfirmed[0]
                Log.d(TAG, "VOCABULARY MATCH ID: $matchId, ALIASES: ${vocMap[matchId]!!}")
            }
        }
        return matchId
    }

}