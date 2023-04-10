package com.kt.apps.core.extensions

import com.kt.apps.core.storage.IKeyValueStorage
import com.kt.apps.core.storage.KeyValueStorageForTesting
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Before
import org.junit.Test


class ParserExtensionsSourceTest {

    private val url = "https://iptv-org.github.io/iptv/index.m3u"
    private lateinit var parserExtensionsSource: ParserExtensionsSource
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var storage: IKeyValueStorage
    private lateinit var config: ExtensionsConfig
    private lateinit var disposable: CompositeDisposable

    @Before
    fun prepare() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        config = ExtensionsConfig(
            "IP TV",
            url
        )
        storage = KeyValueStorageForTesting()
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        parserExtensionsSource = ParserExtensionsSource(okHttpClient, storage)
        disposable = CompositeDisposable()

    }

    @Test
    fun parseFromRemoteRx() {
        parserExtensionsSource.parseFromRemoteRx(config)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { result ->
                result.map {
                    it.logoChannel
                }.contains("https://i.imgur.com/4HNGHEf.png")
            }
    }

    @Test
    fun parseFromRemote() {
        val result = parserExtensionsSource.parseFromRemote(config)
        assert(result.map {
            it.logoChannel
        }.contains("https://i.imgur.com/4HNGHEf.png"))
    }
}