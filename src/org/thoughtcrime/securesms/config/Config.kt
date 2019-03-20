package org.thoughtcrime.securesms.config

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v4.util.LruCache
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by roma on 09.07.2018.
 */
object Config {

    val ADS_PLACEMENT_TAG_FS_MAIN = "FS_OnStart"
    val ADS_PLACEMENT_TAG_FS_CONVERSATION = "FS_Conversation"
    val ADS_PLACEMENT_TAG_FS_CALL_ENDED = "FS_CallEnded"
    val ADS_PLACEMENT_TAG_SB_CONVERSATION = "SB_Conversation"
    val ADS_PLACEMENT_TAG_SB_CONVERSATION_LIST = "SB_ConversationList"


    var primaryColor: Int = 0
    var primaryDarkColor: Int = 0
    var accentColor: Int = 0

    lateinit var chatLightBackgroundImage: String
    lateinit var chatDarkBackgroundImage: String

    val cameraStickers: MutableList<String> = ArrayList()

    private var mMemoryCache: LruCache<String, Bitmap>? = null

    //lateinit var context: Context
    val random = Random()

     private fun manageCache() {
        Log.w("cache", "init")
        mMemoryCache = object : LruCache<String, Bitmap>(20 * 1000 * 1000) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                 return value!!.byteCount
            }
        }
    }


    fun init(context: Context) {
        //this.context = context
        //manageCache()
        try {
            val settings = JSONObject(loadSettings(context))


            val theme = settings.getJSONObject("themeColors")
            primaryColor = readColor(theme, "colorPrimary") ?: 0
            primaryDarkColor = readColor(theme, "colorPrimaryDark") ?: 0
            accentColor = readColor(theme, "colorAccent") ?: 0

            chatLightBackgroundImage = settings.getString("chatLightBackgroundImage") ?: ""
            chatDarkBackgroundImage = settings.getString("chatDarkBackgroundImage") ?: ""

            val tilesArray = settings.getJSONArray("cameraStickers")
            for (i in 0 until tilesArray.length()) {
                cameraStickers.add(tilesArray.getJSONObject(i).getString("path"))
            }

        } catch (e: JSONException) {
            Log.e("Config", "Json parse error: " + e.message)
        } catch (e: IOException) {
            Log.e("Config", "Json read error: " + e.message)
        }
    }

    fun getChatBackgroundImage(context: Context, darkTheme: Boolean) =
        createDrawable(context, if(darkTheme) chatDarkBackgroundImage else chatLightBackgroundImage)

    @Throws(JSONException::class)
    private fun readColor(jsonTheme: JSONObject, name: String): Int? {
        var color: String? = jsonTheme.getNotNullString(name)
        if (color == null || color == "") {
            return null
        }
        if (!color.startsWith("#")) {
            color = "#" + color
        }
        return Color.parseColor(color)
    }

    @Throws(IOException::class)
    private fun loadSettings(context: Context): String? {
        var json: String? = null
        try {
            val inputStream = context.assets.open("settings.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    fun createDrawable(context: Context, link: String): Drawable? {
        if (link != "") {
            try {
                var b: Bitmap? = mMemoryCache?.get(link)
                if (b == null) {
                    b = BitmapFactory.decodeStream(context.assets.open(link))
                    b!!.density = Bitmap.DENSITY_NONE
                    mMemoryCache?.put(link, b)
                }
                return BitmapDrawable(context.resources, b)
            } catch (e: FileNotFoundException) {
                Log.d("Config", "Image $link not found")
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return null
    }

    fun readBitmap(context: Context, link: String): Bitmap? {
        if (link != "") {
            try {
                var b: Bitmap? = mMemoryCache?.get(link)
                if (b == null) {
                    b = BitmapFactory.decodeStream(context.assets.open(link))
                    b!!.density = Bitmap.DENSITY_NONE
                    mMemoryCache?.put(link, b)
                }
                return b
            } catch (e: FileNotFoundException) {
                Log.d("Config", "Image $link not found")
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return null
    }

    fun readScaledBitmap(context: Context, link: String, width: Int, height: Int): Bitmap? {
        if (link != "") {
            try {
                var b: Bitmap? = mMemoryCache?.get("${link}_${width}_${height}")
                if (b == null) {

                    if (b == null) {
                        b = BitmapFactory.decodeStream(context.assets.open(link))
                        b!!.density = Bitmap.DENSITY_NONE
                        b = BITMAP_RESIZER(b, convertPxToDp(context, width), convertPxToDp(context, height), true)
                    }

                    mMemoryCache?.put("${link}_${width}_${height}", b)
                }
                return b
            } catch (e: FileNotFoundException) {
                Log.d("Config", "Image $link not found")
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return null

    }

    fun BITMAP_RESIZER(bitmap: Bitmap?, newWidth: Int, newHeight: Int, keepAspectRatio: Boolean): Bitmap {
        bitmap?.let {

            var finalWidth = newWidth
            var finalHeight = newHeight

            if (keepAspectRatio) {
                val width = bitmap.width.toFloat()
                val height = bitmap.height.toFloat()
                val ratioBitmap = width / height
                val ratioMax = newWidth.toFloat() / newHeight.toFloat()


                if (ratioMax > ratioBitmap) {
                    finalWidth = (newHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalWidth = (newWidth.toFloat() * ratioBitmap).toInt()
                }
            }

            val ratioX = finalWidth / bitmap.width.toFloat()
            val ratioY = finalHeight / bitmap.height.toFloat()
            val middleX = finalWidth / 2.0f
            val middleY = finalHeight / 2.0f

            val scaleMatrix = Matrix()
            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
            val scaledBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(scaledBitmap)
            canvas.matrix = scaleMatrix
            canvas.drawBitmap(bitmap, middleX - bitmap.width / 2, middleY - bitmap.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            return BitmapFactory.decodeStream(ByteArrayInputStream(byteArray))
        }

        return Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
    }


    fun JSONObject.getNotNullString(key: String) = if (getString(key) == "null") "" else getString(key)

    fun <E> List<E>.random(random: java.util.Random): E? = if (size > 0) get(random.nextInt(size)) else null

    private fun convertPxToDp(context: Context, px: Int): Int =
            (px * context.resources.displayMetrics.density).toInt()
}