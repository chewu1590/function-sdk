package cn.woochen.functionsdk.x5

import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import cn.woochen.functionsdk.R
import com.tencent.smtt.sdk.TbsReaderView
import com.tencent.smtt.sdk.TbsVideo

import java.io.File

class X5WebViewActivity : AppCompatActivity(), TbsReaderView.ReaderCallback {

    private var mTbsReaderView: TbsReaderView? = null
    private var mDownloadBtn: Button? = null

    private var mDownloadManager: DownloadManager? = null
    private var mRequestId: Long = 0
    private var mDownloadObserver: DownloadObserver? = null
    private val mFileUrl = "http://www.beijing.gov.cn/zhuanti/ggfw/htsfwbxzzt/shxfl/fw/P020150720516332194302.doc"
    private val mVideoUrl = "https://www.baidu.com/0517.mp4"
    private var mFileName: String? = null

    private val isLocalExist: Boolean
        get() = localFile.exists()

    private val localFile: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mFileName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_x5_web_view)

        mTbsReaderView = TbsReaderView(this, this)
        mDownloadBtn = findViewById<View>(R.id.btn_download) as Button
        val rootRl = findViewById<View>(R.id.rl_root) as RelativeLayout
        rootRl.addView(mTbsReaderView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        )
        mFileName = parseName(mFileUrl)
        if (isLocalExist) {
            mDownloadBtn!!.text = "打开文件"
        }
    }

    fun onClickDownload(v: View) {
        if (isLocalExist) {
            mDownloadBtn!!.visibility = View.GONE
            displayFile()
        } else {
            startDownload()
        }
    }


    fun onClickPlay(v: View) {
        val canUseTbsPlayer = TbsVideo.canUseTbsPlayer(this)
        Log.e("logx5","播放器是否可用：$canUseTbsPlayer")
        if (canUseTbsPlayer){
            TbsVideo.openVideo(this,mVideoUrl)
        }
    }

    private fun displayFile() {
        val bundle = Bundle()
        bundle.putString("filePath", localFile.path)
        bundle.putString("tempPath", Environment.getExternalStorageDirectory().path)
        val result = mTbsReaderView!!.preOpen(parseFormat(mFileName!!), false)
        if (result) {
            mTbsReaderView!!.openFile(bundle)
        }
    }

    private fun parseFormat(fileName: String): String {
        return fileName.substring(fileName.lastIndexOf(".") + 1)
    }

    private fun parseName(url: String): String {
        var fileName: String? = null
        try {
            fileName = url.substring(url.lastIndexOf("/") + 1)
        } finally {
            if (TextUtils.isEmpty(fileName)) {
                fileName = System.currentTimeMillis().toString()
            }
        }
        return fileName!!
    }

    private fun startDownload() {
        mDownloadObserver = DownloadObserver(Handler())
        contentResolver.registerContentObserver(
            Uri.parse("content://downloads/my_downloads"),
            true,
            mDownloadObserver!!
        )
        mDownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(mFileUrl))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mFileName)
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        mRequestId = mDownloadManager!!.enqueue(request)
    }

    private fun queryDownloadStatus() {
        val query = DownloadManager.Query().setFilterById(mRequestId)
        var cursor: Cursor? = null
        try {
            cursor = mDownloadManager!!.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载的字节数
                val currentBytes =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                //总需下载的字节数
                val totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                //状态所在的列索引
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                Log.i("downloadUpdate: ", "$currentBytes $totalBytes $status")
                mDownloadBtn!!.text = "正在下载：$currentBytes/$totalBytes"
                if (DownloadManager.STATUS_SUCCESSFUL == status && mDownloadBtn!!.visibility == View.VISIBLE) {
                    mDownloadBtn!!.visibility = View.GONE
                    mDownloadBtn!!.performClick()
                }
            }
        } finally {
            cursor?.close()
        }
    }

    override fun onCallBackAction(integer: Int?, o: Any, o1: Any) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mTbsReaderView!!.onStop()
        if (mDownloadObserver != null) {
            contentResolver.unregisterContentObserver(mDownloadObserver!!)
        }
    }

    private inner class DownloadObserver  constructor(handler: Handler) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri) {
            Log.i("downloadUpdate: ", "onChange(boolean selfChange, Uri uri)")
            queryDownloadStatus()
        }
    }
}
