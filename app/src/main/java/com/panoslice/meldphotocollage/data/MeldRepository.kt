package com.panoslice.meldphotocollage.data

import com.panoslice.meldphotocollage.data.db.model.ProjectEntity
import com.panoslice.meldphotocollage.utils.AppResult

interface MeldRepository {
    suspend fun getAllProjects(): List<ProjectEntity>
    suspend fun insertProject(projectEntity: ProjectEntity): Boolean
}