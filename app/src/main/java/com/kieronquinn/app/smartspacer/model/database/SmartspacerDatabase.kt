package com.kieronquinn.app.smartspacer.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kieronquinn.app.smartspacer.utils.room.GsonConverter

@Database(entities = [
    Action::class,
    ActionData::class,
    AppWidget::class,
    BroadcastListener::class,
    ExpandedAppWidget::class,
    ExpandedCustomAppWidget::class,
    Grant::class,
    NotificationListener::class,
    Requirement::class,
    RequirementData::class,
    Target::class,
    TargetData::class,
    Widget::class
], version = 4, exportSchema = false)
@TypeConverters(GsonConverter::class)
abstract class SmartspacerDatabase: RoomDatabase() {

    companion object {
        fun getDatabase(context: Context): SmartspacerDatabase {
            return Room.databaseBuilder(
                context,
                SmartspacerDatabase::class.java,
                "smartspacer"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        }

        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AppWidget ADD COLUMN multi_page INTEGER NOT NULL DEFAULT '0'")
                database.execSQL("ALTER TABLE AppWidget ADD COLUMN show_controls INTEGER NOT NULL DEFAULT '0'")
            }
        }

        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Requirement ADD COLUMN invert INTEGER NOT NULL DEFAULT '0'")
            }
        }

        private val MIGRATION_3_4 = object: Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AppWidget ADD COLUMN animate INTEGER NOT NULL DEFAULT '1'")
            }
        }
    }

    abstract fun actionDao(): ActionDao
    abstract fun actionDataDao(): ActionDataDao
    abstract fun appWidgetDao(): AppWidgetDao
    abstract fun broadcastListenerDao(): BroadcastListenerDao
    abstract fun expandedAppWidgetDao(): ExpandedAppWidgetDao
    abstract fun expandedCustomAppWidgetDao(): ExpandedCustomAppWidgetDao
    abstract fun targetDao(): TargetDao
    abstract fun targetDataDao(): TargetDataDao
    abstract fun grantDao(): GrantDao
    abstract fun notificationListenerDao(): NotificationListenerDao
    abstract fun requirementDao(): RequirementDao
    abstract fun requirementDataDao(): RequirementDataDao
    abstract fun widgetDao(): WidgetDao

}