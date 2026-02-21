package com.zeiterfassung.model.enums

enum class Permission(val value: String) {
    TIME_TRACK_OWN("time.track.own"),
    TIME_EDIT_OWN("time.edit.own"),
    TIME_EDIT_TEAM("time.edit.team"),
    TIME_VIEW_OWN("time.view.own"),
    TIME_VIEW_TEAM("time.view.team"),
    TIME_VIEW_ALL("time.view.all"),
    VACATION_REQUEST_OWN("vacation.request.own"),
    VACATION_APPROVE("vacation.approve"),
    VACATION_VIEW_TEAM("vacation.view.team"),
    VACATION_VIEW_ALL("vacation.view.all"),
    ADMIN_USERS_MANAGE("admin.users.manage"),
    ADMIN_SETTINGS_MANAGE("admin.settings.manage"),
    ADMIN_REPORTS_VIEW("admin.reports.view"),
    CSV_IMPORT("csv.import"),
    CSV_EXPORT("csv.export"),
}
