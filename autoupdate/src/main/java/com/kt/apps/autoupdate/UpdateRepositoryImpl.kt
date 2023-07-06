package com.kt.apps.autoupdate

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kt.apps.autoupdate.exceptions.AppUpdateExceptions
import com.kt.apps.autoupdate.model.UpdateInfo
import com.kt.apps.autoupdate.model.request.AppUpdateRequest
import com.kt.apps.core.storage.IKeyValueStorage
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class UpdateRepositoryImpl @Inject constructor(
    private val client: OkHttpClient,
    private val storage: IKeyValueStorage,
    private val remoteConfig: FirebaseRemoteConfig,
) : IUpdateRepository {

    override fun checkUpdate(appUpdateRequest: AppUpdateRequest): Maybe<UpdateInfo> {
        return Maybe.create<UpdateInfo> { emitter ->
            val updateInfoStr = remoteConfig.getString(EXTRA_KEY_CHECK_UPDATE)
            if (updateInfoStr.isBlank()) {
                emitter.onError(
                    AppUpdateExceptions(
                        AppUpdateExceptions.ERROR_CODE_EMPTY_RESPONSE,
                        "Empty response body"
                    )
                )
                return@create
            }

            val updateInfoJs = JSONObject(updateInfoStr)
            val version = updateInfoJs.optInt("newest_version").takeIf {
                it > appUpdateRequest.currentVersion
            } ?: return@create

            val priority = updateInfoJs.optInt("priority")

            val updateMethodJs: JSONArray = updateInfoJs.optJSONArray("update_method").takeIf {
                it != null && it.length() > 0
            } ?: return@create
            val updateMethod = mutableListOf<UpdateInfo.UpdateMethod>()
            for (index in 0 until updateMethodJs.length()) {
                val item = updateMethodJs.getJSONObject(index)
                updateMethod.add(
                    UpdateInfo.UpdateMethod(
                        item.optString("type"),
                        item.optString("link")
                    )
                )
            }
            storage.save(EXTRA_KEY_CACHE_UPDATE_INFO, updateInfoStr)
            emitter.onSuccess(UpdateInfo(version, priority, updateMethod))
            emitter.onComplete()
        }.retry { times, throwable ->
            return@retry times < 3 && throwable !is AppUpdateExceptions
        }
    }

    companion object {
        const val EXTRA_KEY_CACHE_UPDATE_INFO = "extra:cache_update_info"
        private const val EXTRA_KEY_CHECK_UPDATE = "update_info"
    }

}