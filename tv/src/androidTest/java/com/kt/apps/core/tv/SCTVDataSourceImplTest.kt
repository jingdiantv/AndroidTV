package com.kt.apps.core.tv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.core.Constants
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.datasource.impl.SCTVDataSourceImpl
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.tv.model.TVDataSourceFrom
import com.kt.apps.core.tv.storage.TVStorage
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class SCTVDataSourceImplTest {

    private lateinit var sctvDataSourceImpl: SCTVDataSourceImpl
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var firebaseDataBase: FirebaseDatabase
    private lateinit var keyValueStorage: TVStorage
    private lateinit var roomDataBase: RoomDataBase
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var testChannel: TVChannel

    @Before
    fun prepare() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }

        val context = ApplicationProvider.getApplicationContext<Context>()
        roomDataBase = Room.inMemoryDatabaseBuilder(
            context, RoomDataBase::class.java
        ).build()

        val sharedPreferences = context.getSharedPreferences(
            Constants.SHARE_PREF_NAME,
            Context.MODE_PRIVATE
        )

        okHttpClient = OkHttpClient.Builder()
            .build()

        firebaseDataBase = Mockito.mock(FirebaseDatabase::class.java)
        remoteConfig = Mockito.mock(FirebaseRemoteConfig::class.java)
        keyValueStorage = TVStorage(sharedPreferences)
        sctvDataSourceImpl = SCTVDataSourceImpl(
            okHttpClient, firebaseDataBase, keyValueStorage, roomDataBase, remoteConfig
        )
        testChannel = TVChannel(
            TVChannelGroup.SCTV.name,
            "Test",
            "Test",
            "https://sctvonline.vn/detail/abc-australia-06914b8b",
            TVDataSourceFrom.MAIN_SOURCE.name,
            "abc-australia-06914b8b",
            urls = listOf(
                TVChannel.Url(
                    "sctv",
                    "web",
                    url = "https://sctvonline.vn/detail/abc-australia-06914b8b"
                ),
                TVChannel.Url(
                    "vieon",
                    "web",
                    url = "https://sctvonline.vn/detail/sctv11-1d565c47/"
                )
            )
        )
    }

    @Test
    fun getTvList() {

    }

    @Test
    fun getTvLinkFromDetail() {
        sctvDataSourceImpl.getTvLinkFromDetail(testChannel)
            .test()
            .assertComplete()
            .assertValue {
                println(it)
                println(it.linkStream)
                it.linkStream.isNotEmpty()
            }
    }


    @Test
    fun getMainPageMenu() {
    }
}