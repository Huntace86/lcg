package top.easelink.lcg.ui.main.source

import androidx.annotation.WorkerThread
import io.reactivex.Observable
import top.easelink.lcg.ui.main.source.model.*

/**
 * author : junzhang
 * date   : 2019-07-26 14:59
 * desc   :
 */
interface ArticlesDataSource {
    @WorkerThread
    fun getForumArticles(query: String, processThreadList: Boolean): ForumPage?

    @WorkerThread
    fun getArticleDetail(query: String): ArticleDetail?

    @WorkerThread
    fun getPostPreview(query: String): PreviewPost?

    @WorkerThread
    fun getHomePageArticles(param: String, pageNum: Int): List<Article>
}