package com.example.core.database

import com.example.core.database.entity.TemplateEntity
import org.json.JSONArray

object BuiltInTemplates {

    val defaultTemplates: List<TemplateEntity> by lazy {
        val now = 1700000000000L // Stable timestamp for built-ins
        listOf(
            TemplateEntity(
                id = "template_builtin_meeting",
                name = "Meeting Notes",
                description = "Structured agenda, discussion notes, and action items",
                iconName = "groups",
                defaultTitle = "Meeting: Project Sync",
                contentTemplate = "## Agenda\n• Overview and updates\n• Open discussion\n• Key decisions\n\n## Discussion Notes\n\n\n## Action Items\n• [ ] Follow up on deliverables\n• [ ] Schedule next review",
                isChecklist = 0,
                checklistTemplateJson = null,
                isBuiltIn = 1,
                createdAt = now,
                updatedAt = now
            ),
            TemplateEntity(
                id = "template_builtin_journal",
                name = "Daily Journal",
                description = "Reflect on accomplishments, gratitude, and intentions",
                iconName = "auto_stories",
                defaultTitle = "Daily Reflection",
                contentTemplate = "## Morning Intentions\nWhat will make today productive and fulfilling?\n\n## Gratitude\n1. \n2. \n3. \n\n## Evening Review\nHighlights of the day and takeaways for tomorrow:",
                isChecklist = 0,
                checklistTemplateJson = null,
                isBuiltIn = 1,
                createdAt = now,
                updatedAt = now
            ),
            TemplateEntity(
                id = "template_builtin_weekly_review",
                name = "Weekly Review",
                description = "Evaluate weekly progress, wins, and upcoming priorities",
                iconName = "date_range",
                defaultTitle = "Weekly Planning & Review",
                contentTemplate = "## Key Wins This Week\n• \n• \n\n## Areas for Improvement\n\n\n## Top 3 Priorities for Next Week\n1. \n2. \n3. ",
                isChecklist = 0,
                checklistTemplateJson = null,
                isBuiltIn = 1,
                createdAt = now,
                updatedAt = now
            ),
            TemplateEntity(
                id = "template_builtin_project_brief",
                name = "Project Brief",
                description = "Define scope, goals, milestones, and deliverables",
                iconName = "assignment",
                defaultTitle = "Project: New Initiative",
                contentTemplate = "## Executive Summary\nHigh-level problem statement and proposed solution.\n\n## Core Objectives\n1. \n2. \n\n## Milestones & Schedule\n• Sprint 1: Discovery & Architecture\n• Sprint 2: Core Implementation\n• Sprint 3: Testing & Polish\n\n## Deliverables & Acceptance Criteria\n",
                isChecklist = 0,
                checklistTemplateJson = null,
                isBuiltIn = 1,
                createdAt = now,
                updatedAt = now
            ),
            TemplateEntity(
                id = "template_builtin_daily_checklist",
                name = "Daily Checklist",
                description = "Interactive checklist for daily productivity routines",
                iconName = "checklist",
                defaultTitle = "Daily Focus Checklist",
                contentTemplate = "",
                isChecklist = 1,
                checklistTemplateJson = JSONArray(
                    listOf(
                        "Review top 3 daily priorities",
                        "Deep work session: core focus",
                        "Clear inbox and communications",
                        "Review progress and prepare for tomorrow"
                    )
                ).toString(),
                isBuiltIn = 1,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
