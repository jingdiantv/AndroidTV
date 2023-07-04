package com.kt.apps.core.usecase.search

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.di.CoreScope
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelDBWithCategoryViews
import com.kt.apps.core.storage.local.dto.TVChannelDTO
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

@CoreScope
class SearchForText @Inject constructor(
    private val roomDataBase: RoomDataBase
) : MaybeUseCase<Map<String, List<SearchForText.SearchResult>>>() {

    sealed class SearchResult {
        class ExtensionsChannelWithCategory(
            val data: ExtensionsChannelDBWithCategoryViews,
            val highlightTitle: SpannableString
        ) : SearchResult()

        data class TV(
            val data: TVChannelDTO,
            val urls: List<TVChannelDTO.TVChannelUrl>,
            val highlightTitle: SpannableString
        ) : SearchResult()
    }

    override fun prepareExecute(params: Map<String, Any>): Maybe<Map<String, List<SearchResult>>> {
        val query = params[EXTRA_QUERY] as String
        val limit = params[EXTRA_LIMIT] as Int
        val offset = params[EXTRA_OFFSET] as Int
        val filter = params[EXTRA_FILTER] as? String
        val filterHighlight = query.lowercase()
            .replaceVNCharsToLatinChars()
            .split(" ")
            .filter {
                it.isNotBlank()
            }
        val tvChannelSource: Single<Map<String, List<SearchResult>>> = roomDataBase.tvChannelDao()
            .searchChannelByName(query)
            .map {
                it.map {
                    SearchResult.TV(
                        it.tvChannel, it.urls,
                        getHighlightTitle(it.tvChannel.tvChannelName, filterHighlight)
                    )
                }.groupBy {
                    it.data.tvGroup
                }
            }

        val extensionsSource: Single<Map<String, List<SearchResult>>> = roomDataBase.extensionsChannelDao()
            .searchByNameQuery(getSqlQuery(query, filter, limit, offset))
            .map {
                val list = it.map {
                    SearchResult.ExtensionsChannelWithCategory(
                        it,
                        getHighlightTitle(it.tvChannelName, filterHighlight)
                    )
                }
                list.groupBy {
                    it.data.categoryName
                }
            }

        return when {
            filter == FILTER_ONLY_TV_CHANNEL -> tvChannelSource.toFlowable()
            filter == FILTER_ALL_IPTV -> extensionsSource.toFlowable()
            !filter?.trim().isNullOrBlank() -> {
                extensionsSource.toFlowable()
            }
            else -> tvChannelSource.mergeWith(extensionsSource)
        }.reduce { t1, t2 ->
            val list: MutableMap<String, List<SearchResult>> = t1.toMutableMap()
            list.putAll(t2)
            list
        }
    }

    operator fun invoke(
        query: String,
        filter: String?,
        limit: Int,
        offset: Int
    ) = execute(
        mapOf(
            EXTRA_QUERY to query.lowercase(),
            EXTRA_LIMIT to limit,
            EXTRA_OFFSET to offset,
            EXTRA_FILTER to (filter ?: "")
        )
    )

    private fun getSqlQuery(
        queryString: String,
        filter: String?,
        limit: Int,
        offset: Int
    ): SupportSQLiteQuery {
        val splitStr = queryString.lowercase().split(" ")
            .filter {
                it.isNotBlank()
            }

        var regexSplit = ""
        var oderBy = ""
        var orderCount = 1
        var filterById = ""
        if (!filter?.trim().isNullOrBlank() && filter != FILTER_ALL_IPTV) {
            filterById = " AND configSourceUrl='$filter' "
        }
        oderBy += "ORDER BY CASE WHEN tvChannelName = '$queryString' THEN $orderCount "
        orderCount++
        oderBy += "WHEN tvChannelName LIKE '$queryString%' THEN $orderCount "
        orderCount++
        oderBy += "WHEN tvChannelName LIKE '%$queryString%' THEN $orderCount "
        orderCount++
        if (splitStr.size > 1) {
            regexSplit = splitStr.map {
                return@map "OR tvChannelName MATCH '*$it *' "
            }.reduceIndexed { index, acc, s ->
                if (index == 0) {
                    oderBy += "WHEN tvChannelName LIKE '${splitStr[index]} %' THEN $orderCount "
                    orderCount++
                }
                oderBy += "WHEN tvChannelName MATCH '%${splitStr[index]} %' THEN $orderCount "
                orderCount++
                "$acc$s"
            }
        }

        if (oderBy.isNotBlank()) {
            oderBy+="ELSE $orderCount " +
                    "END "
        }

        val queryStr = "SELECT extensionSourceId as configSourceUrl, tvGroup as categoryName, tvChannelName, " +
                "logoChannel, tvStreamLink, sourceFrom FROM ExtensionsChannelFts4 " +
                "WHERE tvChannelName MATCH '*$queryString*' $regexSplit$filterById" +
                "LIMIT $limit " +
                "OFFSET $offset "

        Logger.d(
            this, message = "Query: {" +
                    "queryStr: $queryStr" +
                    "}"
        )

        return SimpleSQLiteQuery(queryStr)
    }

    companion object {
        private const val EXTRA_QUERY = "extra:query"
        private const val EXTRA_LIMIT = "extra:limit"
        private const val EXTRA_OFFSET = "extra:offset"
        private const val EXTRA_FILTER = "extra:filter"
        const val FILTER_ONLY_TV_CHANNEL = "tv"
        const val FILTER_ALL_IPTV = "all_iptv"
        const val FILTER_FOOT_BALL = "football"
        private val REGEX_VN = Regex(
            "[aáàảãạăắằẳẵặđeéèẻẽẹêếềểễệoóòỏõọôốồổỗộơớờởỡợuúùủũụưứửữựừ]"
        )
        private val mapRegex by lazy {
            mapOf(
                "á" to "[aáàảãạăâ]",
                "à" to "[aáàảãạăâ]",
                "ả" to "[aáàảãạăâ]",
                "ã" to "[aáàảãạăâ]",
                "ạ" to "[aáàảãạăâ]",
                "â" to "[aáàảãạăâ]",
                "ấ" to "[aáàảãạăâ]",
                "ầ" to "[aáàảãạăâ]",
                "ẩ" to "[aáàảãạăâ]",
                "ẫ" to "[aáàảãạăâ]",
                "ậ" to "[aáàảãạăâ]",
                "ă" to "[aáàảãạăâ]",
                "ắ" to "[aáàảãạăâ]",
                "ằ" to "[aáàảãạăâ]",
                "ẳ" to "[aáàảãạăâ]",
                "ẵ" to "[aáàảãạăâ]",
                "ặ" to "[aáàảãạăâ]",
                "đ" to "[dđ]",
                "é" to "[eê]",
                "è" to "[eê]",
                "ẻ" to "[eê]",
                "ẽ" to "[eê]",
                "ẹ" to "[eê]",
                "ê" to "[eê]",
                "ế" to "[eê]",
                "ề" to "[eê]",
                "ể" to "[eê]",
                "ễ" to "[eê]",
                "ệ" to "e",
                "ó" to "o",
                "ò" to "o",
                "ỏ" to "o",
                "õ" to "o",
                "ọ" to "o",
                "ô" to "oz",
                "ố" to "oz",
                "ồ" to "oz",
                "ổ" to "oz",
                "ỗ" to "oz",
                "ộ" to "oz",
                "ơ" to "ozz",
                "ớ" to "ozz",
                "ờ" to "ozz",
                "ở" to "ozz",
                "ỡ" to "ozz",
                "ợ" to "ozz",
                "ư" to "uz",
                "ứ" to "uz",
                "ừ" to "uz",
                "ữ" to "uz",
                "ự" to "uz",
                "ú" to "u",
                "ù" to "u",
                "ủ" to "u",
                "ũ" to "u",
                "ụ" to "u",
            )
        }
        val map by lazy {
            mapOf(
                "á" to "a",
                "à" to "a",
                "ả" to "a",
                "ã" to "a",
                "ạ" to "a",
                "â" to "a",
                "ấ" to "azz",
                "ầ" to "azz",
                "ẩ" to "azz",
                "ẫ" to "azz",
                "ậ" to "azz",
                "ă" to "az",
                "ắ" to "az",
                "ằ" to "az",
                "ẳ" to "az",
                "ẵ" to "az",
                "ặ" to "az",
                "đ" to "dz",
                "é" to "e",
                "è" to "e",
                "ẻ" to "e",
                "ẽ" to "e",
                "ẹ" to "e",
                "ê" to "e",
                "ế" to "e",
                "ề" to "e",
                "ể" to "e",
                "ễ" to "e",
                "ệ" to "e",
                "ó" to "o",
                "ò" to "o",
                "ỏ" to "o",
                "õ" to "o",
                "ọ" to "o",
                "ô" to "oz",
                "ố" to "oz",
                "ồ" to "oz",
                "ổ" to "oz",
                "ỗ" to "oz",
                "ộ" to "oz",
                "ơ" to "ozz",
                "ớ" to "ozz",
                "ờ" to "ozz",
                "ở" to "ozz",
                "ỡ" to "ozz",
                "ợ" to "ozz",
                "ư" to "uz",
                "ứ" to "uz",
                "ừ" to "uz",
                "ữ" to "uz",
                "ự" to "uz",
                "ú" to "u",
                "ù" to "u",
                "ủ" to "u",
                "ũ" to "u",
                "ụ" to "u",
            )
        }

        private val HIGH_LIGHT_COLOR by lazy {
            Color.parseColor("#fb8500")
        }
        private val FOREGROUND_HIGH_LIGHT_COLOR by lazy {
            ForegroundColorSpan(HIGH_LIGHT_COLOR)
        }

        fun getHighlightTitle(realTitle: String, _filterHighlight: List<String>?): SpannableString {
            val spannableString = SpannableString(realTitle)
            val lowerRealTitle = realTitle.trim()
                .lowercase()
                .replaceVNCharsToLatinChars()

            val titleLength = lowerRealTitle.length
            _filterHighlight?.forEach { searchKey ->
                var index = lowerRealTitle.indexOf(searchKey)
                while (index > -1 && index + searchKey.length <= titleLength) {
                    spannableString.setSpan(
                        ForegroundColorSpan(HIGH_LIGHT_COLOR),
                        index,
                        index + searchKey.length,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                    val startIndex = index + searchKey.length
                    if (startIndex >= titleLength) {
                        break
                    }
                    index = lowerRealTitle.indexOf(searchKey, startIndex)
                }
            }
            return spannableString
        }
    }

}