package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM note_templates ORDER BY is_built_in DESC, created_at ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM note_templates ORDER BY is_built_in DESC, created_at ASC")
    suspend fun getAllTemplatesSync(): List<TemplateEntity>

    @Query("SELECT * FROM note_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)

    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    @Query("DELETE FROM note_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("SELECT COUNT(*) FROM note_templates WHERE is_built_in = 1")
    suspend fun getBuiltInTemplateCount(): Int
}
