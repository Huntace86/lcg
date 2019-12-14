package top.easelink.lcg.ui.main.article.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.easelink.framework.base.BaseFragment
import top.easelink.lcg.BR
import top.easelink.lcg.R
import top.easelink.lcg.databinding.FragmentArticleBinding
import top.easelink.lcg.mta.EVENT_OPEN_ARTICLE
import top.easelink.lcg.mta.sendEvent
import top.easelink.lcg.ui.main.article.view.DownloadLinkDialog.Companion.newInstance
import top.easelink.lcg.ui.main.article.view.ReplyPostDialog.Companion.newInstance
import top.easelink.lcg.ui.main.article.view.ScreenCaptureDialog.Companion.TAG
import top.easelink.lcg.ui.main.article.viewmodel.ArticleAdapter
import top.easelink.lcg.ui.main.article.viewmodel.ArticleViewModel
import top.easelink.lcg.ui.main.articles.viewmodel.ArticleFetcher
import top.easelink.lcg.ui.main.model.ReplyPostEvent
import top.easelink.lcg.ui.main.model.ScreenCaptureEvent
import top.easelink.lcg.ui.webview.view.WebViewActivity
import top.easelink.lcg.utils.WebsiteConstant
import top.easelink.lcg.utils.showMessage
import java.util.*

class ArticleFragment : BaseFragment<FragmentArticleBinding, ArticleViewModel>() {
    private var articleUrl: String? = null
    override fun getBindingVariable(): Int {
        return BR.viewModel
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_article
    }

    override fun getViewModel(): ArticleViewModel {
        return ViewModelProviders.of(this).get(ArticleViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        arguments?.run {
            articleUrl = getString(KEY_URL)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUp()
        setHasOptionsMenu(true)
        (activity as AppCompatActivity?)?.setSupportActionBar(viewDataBinding.articleToolbar)
        viewModel.setUrl(articleUrl!!)
        viewModel.fetchArticlePost(ArticleFetcher.FETCH_INIT)
    }

    override fun onDetach() {
        super.onDetach()
        EventBus.getDefault().unregister(this)
    }

    private fun setUp() {
        viewDataBinding.backToTop.setOnClickListener {
            scrollToTop()
        }
        viewDataBinding.postRecyclerView.apply {
            val mLayoutManager = LinearLayoutManager(context).also {
                it.orientation = RecyclerView.VERTICAL
            }
            layoutManager = mLayoutManager
            itemAnimator = DefaultItemAnimator()
            adapter = ArticleAdapter(viewModel)

            viewModel.posts.observe(this@ArticleFragment, androidx.lifecycle.Observer{
                (adapter as? ArticleAdapter)?.run {
                    clearItems()
                    addItems(it)
                }
            })
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int
                ) {
                    val firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition()
                    val backToTop = viewDataBinding!!.backToTop
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (firstVisibleItemPosition == 0) {
                            backToTop.visibility = View.GONE
                            backToTop.pauseAnimation()
                        } else {
                            backToTop.visibility = View.VISIBLE
                        }
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        backToTop.visibility = View.GONE
                        backToTop.pauseAnimation()
                    }
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.article, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open_in_webview -> WebViewActivity.startWebViewWith(
                WebsiteConstant.SERVER_BASE_URL + articleUrl,
                context
            )
            R.id.action_extract_urls -> {
                val linkList: ArrayList<String>? = viewModel.extractDownloadUrl()
                if (linkList != null && linkList.isNotEmpty()) {
                    newInstance(linkList).show(fragmentManager)
                } else {
                    showMessage(R.string.download_link_not_found)
                }
            }
            R.id.action_add_to_my_favorite -> viewModel.addToFavorite()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scrollToTop() {
        viewDataBinding.postRecyclerView.smoothScrollToPosition(0)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ReplyPostEvent) {
        newInstance(event.replyUrl, event.author).show(fragmentManager)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ScreenCaptureEvent) {
        ScreenCaptureDialog.newInstance(event.imagePath).show(fragmentManager!!, TAG)
    }

    companion object {
        private const val KEY_URL = "KEY_URL"
        @JvmStatic
        fun newInstance(url: String): ArticleFragment {
            sendEvent(EVENT_OPEN_ARTICLE)
            return ArticleFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_URL, url)
                }
            }
        }
    }
}