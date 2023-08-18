package com.panoslice.meldphotocollage.data

import android.content.Context
import com.panoslice.meldphotocollage.data.db.dao.ProjectDao
import com.panoslice.meldphotocollage.data.db.model.ProjectEntity

class MeldRepositoryImpl(private val context: Context,
                         private val projectDao: ProjectDao) : MeldRepository {
    override suspend fun getAllProjects(): List<ProjectEntity> {
        return  projectDao.findAll();
    }

    override suspend fun insertProject(projectEntity: ProjectEntity): Boolean {
        projectDao.add(projectEntity)
        return true
    }

}