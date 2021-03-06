package com.example.chatt

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.chatt.DBHelper.DBAdapter
import com.example.chatt.Server.SocketServer
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_profile_my.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProfileActivity : AppCompatActivity() {

    lateinit var roomId: String
    lateinit var friendId: String
    lateinit var friendName: String
    var isRoom: Boolean = false
    lateinit var mSocket: Socket

    lateinit var db: DBAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = DBAdapter(this)

        var getIntent = intent.extras
        friendName = getIntent!!.getString("friendName")!!
        friendId = getIntent.getString("friendObjectId")!!


        var imgFile: File = File("/storage/emulated/0/isdown"+"/profile_$friendId.jpg")
        if(imgFile.exists()){
            var myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            profile_imageview_profileimg?.setImageBitmap(myBitmap)
        }
        profile_textview_username.text = friendName

        mSocket = SocketServer.getSocket()
        mSocket.connect()
        mSocket.on("new room", onNewRoom())

        Log.e(this.toString(), "소켓아디" + mSocket.id())


        profile_imgButton_close.setOnClickListener {
            finish()
        }

        profile_imagebtn_chat.setOnClickListener {


            var intent = Intent(this, ChatActivity::class.java)

            if (checkRoom(friendId) == "") {
                isRoom = false

                var objArray = JSONArray()
                var obj: JSONObject = JSONObject()
                obj.put("user1", getUserIdFromShared())
                obj.put("user2", friendId)

                Log.e(this.toString(), "request room" + mSocket.emit("add room", obj))

            } else {
                isRoom = false

                intent.putExtra("roomId", checkRoom(friendId))
                intent.putExtra("userName", friendName)

                Log.e(this.toString(), "room already here")

                startActivity(intent)
                finish()

            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()

        mSocket.off("new room", onNewRoom())

    }

    private fun checkRoom(friendId: String?): String {

        db.openDB()

        val c: Cursor = db.AllRooms
        while (c.moveToNext()) {
            var otherUsersId = c.getString(4)

            if (otherUsersId == friendId) {
                roomId = c.getString(1)

                isRoom = true
                Log.e(this.toString(), "room is checked")
            }
        }
        if (!isRoom) {
            roomId = ""
            Log.e(this.toString(), "room is not checked")

        }
        return roomId
    }

    private fun addRoomToSQLite(roomId: String, state: String, otherUsersId: String) {
        db.openDB()

        val result = db.addRoom(roomId, state, null, otherUsersId)
        if (result > 0) {
            Log.i("mytag", "result: $result")
        } else {

        }
        db.close()

    }

    private fun addRoomUserToSQLite(room: String) {

        db.openDB()

        print("friendid: $friendId")
        var result = db.addRoomUser(room, friendId, friendName)

        if (result > 0) {
            Log.e(this.toString(), "add RoomUserToSQLite! result: $result")
        }else{
            Log.e(this.toString(), "addRoomUser Errors")
        }

        db.close()

    }

    private fun getUserIdFromShared(): String {

        var shared: SharedPreferences =
            getSharedPreferences("USERSIGN", Context.MODE_PRIVATE)

        return shared.getString("id", null)!!
    }

    private fun onNewRoom(): Emitter.Listener = Emitter.Listener { args ->
        runOnUiThread(Runnable {

            var data: JSONObject = args[0] as JSONObject
            var room = data.getString("id")
            Log.e(this.toString(), "newRoom made: " + data.getString("id"))

            Log.e(this.toString(),data.getString("id") )
            addRoomToSQLite(data.getString("id"), "one", friendId)
            addRoomUserToSQLite(room)

            var intent = Intent(applicationContext, ChatActivity::class.java)
            intent.putExtra("roomId", data.getString("id"))
            startActivity(intent)

            finish()
        })

    }

}
