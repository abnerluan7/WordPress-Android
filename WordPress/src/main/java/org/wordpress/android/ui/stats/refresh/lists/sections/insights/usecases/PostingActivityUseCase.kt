package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.stats.insights.PostingActivityStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.StatelessUseCase
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class PostingActivityUseCase
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val store: PostingActivityStore,
    private val postingActivityMapper: PostingActivityMapper
) : StatelessUseCase<PostingActivityModel>(POSTING_ACTIVITY, mainDispatcher) {
    override fun buildLoadingItem(): List<BlockListItem> = listOf(Title(R.string.stats_insights_all_time_stats))

    override suspend fun loadCachedData(site: SiteModel): PostingActivityModel? {
        return store.getPostingActivity(site, getStartDate(), getEndDate())
    }

    private fun getEndDate(): Day {
        val endDate = Calendar.getInstance()
        return Day(
                endDate.get(Calendar.YEAR),
                endDate.get(Calendar.MONTH),
                endDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        )
    }

    private fun getStartDate(): Day {
        val startDate = Calendar.getInstance()
        startDate.add(Calendar.MONTH, -2)
        return Day(
                startDate.get(Calendar.YEAR),
                startDate.get(Calendar.MONTH),
                startDate.getActualMinimum(Calendar.DAY_OF_MONTH)
        )
    }

    override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): State<PostingActivityModel> {
        val response = store.fetchPostingActivity(site, getStartDate(), getEndDate(), forced)
        val model = response.model
        val error = response.error

        return when {
            error != null -> State.Error(error.message ?: error.type.name)
            model != null && model.months.isNotEmpty() -> State.Data(
                    model
            )
            else -> State.Empty()
        }
    }

    override fun buildUiModel(domainModel: PostingActivityModel): List<BlockListItem> {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_posting_activity))
        val activityItem = postingActivityMapper.buildActivityItem(domainModel.months, domainModel.max)
        items.add(activityItem)
        return items
    }
}
