package com.henryia.app.core

import android.content.Context
import com.henryia.app.ai.GeneratedProject
import com.henryia.app.ai.GeneratedProjectFile
import com.henryia.app.ai.ProjectType
import org.json.JSONArray
import org.json.JSONObject

class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("henry_projects", 0)

    fun save(project: GeneratedProject) {
        val array = JSONArray()
        loadAll().filterNot { it.name == project.name }.forEach { array.put(toJson(it)) }
        array.put(toJson(project))
        prefs.edit().putString("projects", array.toString()).apply()
    }

    fun loadAll(): List<GeneratedProject> {
        val raw = prefs.getString("projects", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i)))
            }
        }.getOrElse { emptyList() }
    }

    fun clear() { prefs.edit().remove("projects").apply() }

    private fun toJson(project: GeneratedProject): JSONObject =
        JSONObject().put("name", project.name).put("type", project.type.name)
            .put("description", project.description)
            .put("files", JSONArray().apply {
                project.files.forEach { put(JSONObject().put("path", it.path).put("content", it.content)) }
            })

    private fun fromJson(json: JSONObject): GeneratedProject {
        val files = json.optJSONArray("files") ?: JSONArray()
        return GeneratedProject(
            json.optString("name", "Projeto Henry"),
            runCatching { ProjectType.valueOf(json.optString("type")) }.getOrDefault(ProjectType.UNKNOWN),
            json.optString("description"),
            buildList {
                for (i in 0 until files.length()) {
                    val f = files.optJSONObject(i) ?: continue
                    add(GeneratedProjectFile(f.optString("path"), f.optString("content")))
                }
            }
        )
    }
}
