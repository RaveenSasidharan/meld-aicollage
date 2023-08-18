package com.panoslice.meldphotocollage.data.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
class ProjectEntity ( @PrimaryKey(autoGenerate = true) val id: Int)


