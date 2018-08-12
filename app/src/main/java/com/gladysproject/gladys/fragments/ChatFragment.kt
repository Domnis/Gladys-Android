package com.gladysproject.gladys.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.gladysproject.gladys.R
import com.gladysproject.gladys.adapters.ChatAdapter
import com.gladysproject.gladys.models.Message
import com.gladysproject.gladys.utils.Connectivity
import com.gladysproject.gladys.utils.RetrofitAPI
import com.gladysproject.gladys.utils.SelfSigningClientBuilder
import kotlinx.android.synthetic.main.fragment_chat.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ChatFragment : Fragment() {

    private var token :String = ""
    private lateinit var retrofit: Retrofit

    companion object {
        fun newInstance() = ChatFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onStart() {
        super.onStart()

        retrofit = Retrofit.Builder()
                .baseUrl(getConnection()) // The function getConnection return string address
                .addConverterFactory(MoshiConverterFactory.create())
                .client(SelfSigningClientBuilder.unsafeOkHttpClient)
                .build()

        token = PreferenceManager.getDefaultSharedPreferences(context).getString("token", "")!!

        getMessages()

        send_button.setOnClickListener {
            if (message.text.isNotEmpty() || message.text != null) sendMessage(message.text.toString())
        }

    }

    private fun getMessages(){
        retrofit
                .create(RetrofitAPI::class.java)
                .getMessages(token)
                .enqueue(object : Callback<List<Message>> {

                    override fun onResponse(call: Call<List<Message>>, response: Response<List<Message>>) {
                        if(response.code() == 200) refreshView(response.body()!!)
                    }

                    override fun onFailure(call: Call<List<Message>>, err: Throwable) {
                        println(err.message)
                    }
                })
    }

    private fun sendMessage(text : String){
        retrofit
                .create(RetrofitAPI::class.java)
                .sendMessage(text ,"0" ,token)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if(response.code() == 200) {
                            getMessages()
                            message.setText("")
                        }
                    }
                    override fun onFailure(call: Call<Void>, err: Throwable) {
                        println(err.message)
                    }
                })
    }

    fun refreshView(data : List<Message>){
        if(chat_rv != null){
            chat_rv.layoutManager = LinearLayoutManager(context)
            val adapter = ChatAdapter(data)
            chat_rv.adapter = adapter
            chat_rv.scrollToPosition(adapter.itemCount-1)
        }
    }

    /*
     Get type of connection
     0 for no connection
     1 for local connection
     2 for external connection

     See Connectivity file in utils folder for more info
    */
    private fun getConnection(): String {
        return when(context?.let { Connectivity.getTypeOfConnection(it) }){
            1 -> Connectivity.getLocalPreferences(context!!)
            2 -> Connectivity.getNatPreferences(context!!)
            else -> "http://fakeurl"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.toolbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}
