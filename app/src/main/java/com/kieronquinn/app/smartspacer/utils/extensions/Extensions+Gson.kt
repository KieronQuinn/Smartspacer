package com.kieronquinn.app.smartspacer.utils.extensions

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class Exclude

val gsonExclusionStrategy = object: ExclusionStrategy {
   override fun shouldSkipClass(clazz: Class<*>?): Boolean {
      return false
   }

   override fun shouldSkipField(f: FieldAttributes): Boolean {
      return f.getAnnotation(Exclude::class.java) != null
   }
}