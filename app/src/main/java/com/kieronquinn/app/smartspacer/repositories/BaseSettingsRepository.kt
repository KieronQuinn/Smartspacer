package com.kieronquinn.app.smartspacer.repositories

import android.content.SharedPreferences
import android.graphics.Color
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepository.SmartspacerSetting
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_BOOLEAN
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_COLOR
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_DOUBLE
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_FLOAT
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_INT
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_LONG
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.DESERIALIZE_STRING
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_BOOLEAN
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_COLOR
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_DOUBLE
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_FLOAT
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_INT
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_LONG
import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepositoryImpl.SettingsConverters.SHARED_STRING
import com.kieronquinn.app.smartspacer.utils.extensions.toColorOrNull
import com.kieronquinn.app.smartspacer.utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface BaseSettingsRepository {

    val sharedPreferences: SharedPreferences

    suspend fun getBackup(): Map<String, String>
    suspend fun restoreBackup(settings: Map<String, String>)

    abstract class SmartspacerSetting<T> {
        abstract val type: Class<T>

        abstract suspend fun exists(): Boolean
        abstract fun existsSync(): Boolean
        abstract suspend fun set(value: T)
        abstract suspend fun get(): T
        abstract suspend fun getOrNull(): T?
        abstract suspend fun clear()
        abstract fun setSync(value: T)
        abstract fun getSync(): T
        abstract fun asFlow(): Flow<T>
        abstract fun asFlowNullable(): Flow<T?>
        abstract fun key(): String

        abstract suspend fun serialize(): String?
        abstract suspend fun deserialize(serialized: String)
    }

    /**
     *  Helper implementation of [SmartspacerSetting] that takes a regular StateFlow and calls a method
     *  ([onSet]) when [set] is called, allowing for external data to be handled by regular switch
     *  items. [clear] is not implemented, [exists] and [existsSync] will always return true.
     */
    open class FakeSmartspacerSetting<T>(
        override val type: Class<T>,
        private val flow: StateFlow<T>,
        private val onSet: suspend (value: T) -> Unit
    ): SmartspacerSetting<T>() {

        override fun getSync(): T {
            return flow.value
        }

        override fun asFlow(): Flow<T> {
            return flow
        }

        override fun asFlowNullable(): Flow<T?> {
            throw RuntimeException("Not implemented!")
        }

        override suspend fun set(value: T) {
            onSet(value)
        }

        override fun setSync(value: T) {
            runBlocking {
                onSet(value)
            }
        }

        override suspend fun get(): T {
            return flow.value
        }

        override suspend fun getOrNull(): T? {
            return if(exists()){
                get()
            }else{
                null
            }
        }

        override suspend fun exists(): Boolean {
            return true
        }

        override fun existsSync(): Boolean {
            return true
        }

        override suspend fun clear() {
            if(type == String::class.java) {
                (this as FakeSmartspacerSetting<String>).set("")
            }else{
                throw RuntimeException("Not implemented!")
            }
        }

        override suspend fun serialize(): String {
            throw RuntimeException("Not implemented!")
        }

        override suspend fun deserialize(serialized: String) {
            throw RuntimeException("Not implemented!")
        }

        override fun key(): String {
            throw RuntimeException("Not implemented!")
        }

    }

}

abstract class BaseSettingsRepositoryImpl: BaseSettingsRepository {

    fun boolean(key: String, default: Boolean, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            Boolean::class.java,
            key,
            default,
            SHARED_BOOLEAN,
            onChanged,
            SettingsConverters::serializeDefault,
            DESERIALIZE_BOOLEAN
        )

    fun string(key: String, default: String, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            String::class.java,
            key,
            default,
            SHARED_STRING,
            onChanged,
            SettingsConverters::serializeDefault,
            DESERIALIZE_STRING
        )

    fun long(key: String, default: Long, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            Long::class.java,
            key,
            default,
            SHARED_LONG,
            onChanged,
            SettingsConverters::serializeDefault,
            DESERIALIZE_LONG
        )

    fun double(key: String, default: Double, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            Double::class.java,
            key,
            default,
            SHARED_DOUBLE,
            onChanged,
            SettingsConverters::serializeDefault,
            DESERIALIZE_DOUBLE
        )

    fun float(key: String, default: Float, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            Float::class.java,
            key,
            default,
            SHARED_FLOAT,
            onChanged,
            SettingsConverters::serializeDefault,
            DESERIALIZE_FLOAT
        )

    fun int(key: String, default: Int, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            Integer.TYPE,
            key,
            default,
            SHARED_INT,
            onChanged,
            SettingsConverters::serializeDefault,
            DESERIALIZE_INT
        )

    fun color(key: String, default: Int, onChanged: MutableSharedFlow<String>? = null) =
        SmartspacerSettingImpl(
            Integer.TYPE,
            key,
            default,
            SHARED_COLOR,
            onChanged,
            SettingsConverters::serializeColor,
            DESERIALIZE_COLOR
        )

    inline fun <reified T: Enum<T>> enum(
        key: String,
        default: T,
        onChanged: MutableSharedFlow<String>? = null
    ) = SmartspacerSettingImpl(
        T::class.java,
        key,
        default,
        { _, enumKey, enumDefault -> sharedEnum(enumKey, enumDefault) },
        onChanged,
        SettingsConverters::serializeEnum,
        SettingsConverters::deserializeEnum
    )

    private fun shared(key: String, default: Boolean) = ReadWriteProperty({
        sharedPreferences.getBoolean(key, default)
    }, {
        sharedPreferences.edit().putBoolean(key, it).commit()
    })

    private fun shared(key: String, default: String) = ReadWriteProperty({
        sharedPreferences.getString(key, default) ?: default
    }, {
        sharedPreferences.edit().putString(key, it).commit()
    })

    private fun sharedNullableString(key: String, ignored: String) = ReadWriteProperty({
        sharedPreferences.getString(key, "")?.takeIf { it.isNotEmpty() }
    }, {
        sharedPreferences.edit().putString(key, it).commit()
    })

    private fun shared(key: String, default: Int) = ReadWriteProperty({
        sharedPreferences.getInt(key, default)
    }, {
        sharedPreferences.edit().putInt(key, it).commit()
    })

    private fun shared(key: String, default: Float) = ReadWriteProperty({
        sharedPreferences.getFloat(key, default)
    }, {
        sharedPreferences.edit().putFloat(key, it).commit()
    })

    private fun shared(key: String, default: Long) = ReadWriteProperty({
        sharedPreferences.getLong(key, default)
    }, {
        sharedPreferences.edit().putLong(key, it).commit()
    })

    private fun shared(key: String, default: Double) = ReadWriteProperty({
        sharedPreferences.getString(key, default.toString())!!.toDouble()
    }, {
        sharedPreferences.edit().putString(key, it.toString()).commit()
    })

    private fun sharedColor(key: String, unusedDefault: Int) = ReadWriteProperty({
        val rawColor = sharedPreferences.getString(key, "") ?: ""
        if(rawColor.isEmpty()) Integer.MAX_VALUE
        else Color.parseColor(rawColor)
    }, {
        sharedPreferences.edit().putString(key, it.toHexString()).commit()
    })

    object SettingsConverters {
        internal val SHARED_INT: (BaseSettingsRepositoryImpl, String, Int) -> ReadWriteProperty<Any?, Int> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_STRING: (BaseSettingsRepositoryImpl, String, String) -> ReadWriteProperty<Any?, String> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_BOOLEAN: (BaseSettingsRepositoryImpl, String, Boolean) -> ReadWriteProperty<Any?, Boolean> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_FLOAT: (BaseSettingsRepositoryImpl, String, Float) -> ReadWriteProperty<Any?, Float> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_LONG: (BaseSettingsRepositoryImpl, String, Long) -> ReadWriteProperty<Any?, Long> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_DOUBLE: (BaseSettingsRepositoryImpl, String, Double) -> ReadWriteProperty<Any?, Double> =
            BaseSettingsRepositoryImpl::shared
        internal val SHARED_COLOR: (BaseSettingsRepositoryImpl, String, Int) -> ReadWriteProperty<Any?, Int> =
            BaseSettingsRepositoryImpl::sharedColor

        internal val DESERIALIZE_INT = { input: String -> input.toIntOrNull() }
        internal val DESERIALIZE_STRING = { input: String -> input }
        internal val DESERIALIZE_BOOLEAN = { input: String -> input.toBooleanStrictOrNull() }
        internal val DESERIALIZE_FLOAT = { input: String -> input.toFloatOrNull() }
        internal val DESERIALIZE_LONG = { input: String -> input.toLongOrNull() }
        internal val DESERIALIZE_DOUBLE = { input: String -> input.toDoubleOrNull() }
        internal val DESERIALIZE_COLOR = { input: String -> input.toColorOrNull() }

        internal fun <T> serializeDefault(value: T?): String? {
            return value?.toString()
        }

        internal fun <T> serializeColor(value: T?): String? {
            return (value as? Int)?.toHexString()
        }

        inline fun <reified T: Enum<T>> serializeEnum(value: T?): String? {
            return value?.name
        }

        inline fun <reified T: Enum<T>> deserializeEnum(value: String): T? {
            return try {
                enumValueOf<T>(value)
            }catch (e: Exception){
                null
            }
        }

    }

    inner class SmartspacerSettingImpl<T>(
        override val type: Class<T>,
        private val key: String,
        private val default: T,
        shared: (BaseSettingsRepositoryImpl, String, T) -> ReadWriteProperty<Any?, T>,
        private val onChange: MutableSharedFlow<String>? = null,
        private val serializeImpl: (T?) -> String?,
        private val deserializeImpl: (String) -> T?
    ) : SmartspacerSetting<T>() {

        private var rawSetting by shared(this@BaseSettingsRepositoryImpl, key, default)

        override suspend fun exists(): Boolean {
            return withContext(Dispatchers.IO) {
                sharedPreferences.contains(key)
            }
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun existsSync(): Boolean {
            return runBlocking {
                exists()
            }
        }

        override suspend fun set(value: T) {
            withContext(Dispatchers.IO) {
                rawSetting = value
                onChange?.emit(key)
            }
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun setSync(value: T) {
            rawSetting = value
            onChange?.tryEmit(key)
        }

        override suspend fun get(): T {
            return withContext(Dispatchers.IO) {
                rawSetting ?: default
            }
        }

        override suspend fun getOrNull(): T? {
            return if(exists()){
                get()
            }else null
        }

        /**
         *  Should only be used where there is no alternative
         */
        override fun getSync(): T {
            return runBlocking {
                get()
            }
        }

        override suspend fun clear() {
            withContext(Dispatchers.IO) {
                sharedPreferences.edit().remove(key).commit()
            }
        }

        override fun asFlow() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if(key == this@SmartspacerSettingImpl.key) {
                    trySend(rawSetting ?: default)
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(rawSetting ?: default)
            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(Dispatchers.IO)

        override fun asFlowNullable() = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if(key == this@SmartspacerSettingImpl.key) {
                    trySend(rawSetting)
                }
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            if(existsSync()) trySend(rawSetting)
            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(Dispatchers.IO)

        override fun key(): String {
            return key
        }

        override suspend fun serialize(): String? {
            val value = if(exists()){
                get()
            }else null
            return serializeImpl(value)
        }

        override suspend fun deserialize(serialized: String) {
            val value = deserializeImpl(serialized)
            value?.let {
                set(it)
            }
        }

    }

    inline fun <reified T : Enum<T>> sharedEnum(
        key: String,
        default: Enum<T>
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return java.lang.Enum.valueOf(
                    T::class.java,
                    sharedPreferences.getString(key, default.name)
                )
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                sharedPreferences.edit().putString(key, value.name).commit()
            }

        }
    }

    protected inline fun <reified T> sharedList(
        key: String,
        default: List<T>,
        crossinline transform: (List<T>) -> String,
        crossinline reverseTransform: (String) -> List<T>
    ) = ReadWriteProperty({
        reverseTransform(sharedPreferences.getString(key, null) ?: transform(default))
    }, {
        sharedPreferences.edit().putString(key, transform(it)).commit()
    })

    private fun stringListTypeConverter(list: List<String>): String {
        if (list.isEmpty()) return ""
        return list.joinToString(",")
    }

    private fun stringListTypeReverseConverter(pref: String): List<String> {
        if (pref.isEmpty()) return emptyList()
        if (!pref.contains(",")) return listOf(pref.trim())
        return pref.split(",")
    }

    protected inline fun <T> ReadWriteProperty(
        crossinline getValue: () -> T,
        crossinline setValue: (T) -> Unit
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {

            override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return getValue.invoke()
            }

            override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                setValue.invoke(value)
            }

        }
    }

}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class IgnoreInBackup

suspend fun SmartspacerSetting<Boolean>.invert() {
    set(!get())
}