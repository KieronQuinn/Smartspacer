package com.kieronquinn.app.smartspacer.utils.glide

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory

/**
 *  Glide modules genericised using lambdas for components.
 */
fun <I, O, A> Registry.prepend(
    context: Context,
    classIn: Class<I>,
    classOut: Class<O>,
    fetcherArg: A,
    loader: (context: Context, item: I, arg: A, callback: DataFetcher.DataCallback<in O>) -> Unit,
    cacheKey: (I) -> Key,
    handles: (I) -> Boolean = { true },
    dataSource: DataSource = DataSource.LOCAL
) {
    prepend(classIn, classOut, createFactory(
        context, classOut, fetcherArg, loader, cacheKey, handles, dataSource
    ))
}

private fun <I, O, A> createFactory(
    context: Context,
    classOut: Class<O>,
    fetcherArg: A,
    loader: (context: Context, item: I, arg: A, callback: DataFetcher.DataCallback<in O>) -> Unit,
    cacheKey: (I) -> Key,
    handles: (I) -> Boolean,
    dataSource: DataSource
): ModelLoaderFactory<I, O> {
    return object: ModelLoaderFactory<I, O> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<I, O> {
            return createLoader(context, classOut, loader, fetcherArg, cacheKey, handles, dataSource)
        }

        override fun teardown() {
            //No-op
        }
    }
}

private fun <I, O, A> createLoader(
    context: Context,
    classOut: Class<O>,
    loader: (context: Context, item: I, arg: A, callback: DataFetcher.DataCallback<in O>) -> Unit,
    fetcherArg: A,
    cacheKey: (I) -> Key,
    handles: (I) -> Boolean,
    dataSource: DataSource
): ModelLoader<I, O> {
    return object: ModelLoader<I, O> {
        override fun buildLoadData(
            model: I & Any,
            width: Int,
            height: Int,
            options: Options
        ): ModelLoader.LoadData<O> {
            return ModelLoader.LoadData(
                cacheKey(model),
                createFetcher(context, classOut, loader, model, fetcherArg, dataSource)
            )
        }

        override fun handles(model: I & Any): Boolean {
            return handles(model)
        }
    }
}

private fun <I, O, A> createFetcher(
    context: Context,
    classOut: Class<O>,
    loader: (context: Context, item: I, arg: A, callback: DataFetcher.DataCallback<in O>) -> Unit,
    model: I,
    fetcherArg: A,
    dataSource: DataSource
): DataFetcher<O> {
    return object: DataFetcher<O> {
        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in O>) {
            return loader(context, model, fetcherArg, callback)
        }

        override fun cleanup() {
            //No-op
        }

        override fun cancel() {
            //No-op
        }

        override fun getDataClass(): Class<O> {
            return classOut
        }

        override fun getDataSource(): DataSource {
            return dataSource
        }
    }
}