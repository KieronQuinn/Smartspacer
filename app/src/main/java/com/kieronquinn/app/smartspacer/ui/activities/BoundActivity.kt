package com.kieronquinn.app.smartspacer.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.kieronquinn.monetcompat.app.MonetCompatActivity

abstract class BoundActivity<V: ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> V): MonetCompatActivity() {

    private var _binding: V? = null

    protected val binding: V
        get() = _binding ?: throw NullPointerException("Unable to access binding before onCreate or after onDestroy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflate(layoutInflater, window.decorView as ViewGroup, false)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}