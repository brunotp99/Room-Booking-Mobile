package com.example.appmobile

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.activitiesprojeto.ui.theme.*
import com.himanshoe.kalendar.common.KalendarKonfig
import com.himanshoe.kalendar.common.KalendarSelector
import com.himanshoe.kalendar.common.KalendarStyle
import com.himanshoe.kalendar.common.data.KalendarEvent
import com.himanshoe.kalendar.common.theme.Grid
import com.himanshoe.kalendar.ui.Kalendar
import com.himanshoe.kalendar.ui.KalendarType
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import java.util.*

class FireyCalendar : DialogFragment() {

    var listEvents = ArrayList<KalendarEvent>()
    lateinit var hData : String
    var Data = ""
    fun setEvents(list : ArrayList<KalendarEvent>){
        listEvents = list
    }

    fun setValues(data : String){
        hData = data
    }

    public interface MyCallback {
        fun onListClick(data: String)
    }

    var mItemClickListener: MyCallback? = null
    fun setOnClickListener(click: MyCallback) {
        mItemClickListener = click
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_firey_calendar, container, false).apply {
            /* Apresentar o calendario da livraria Kalendar atraves de JetPack Compose */
            val pt = Locale("pt", "PT")
                findViewById<ComposeView>(R.id.horizontalCalendar).setContent {
                    Kalendar(
                        kalendarType = KalendarType.Firey(),
                        kalendarEvents = listEvents,
                        kalendarKonfig = KalendarKonfig(locale = pt),
                        kalendarStyle = KalendarStyle(hasRadius = false, elevation = Grid.Zero, kalendarBackgroundColor = Grey, kalendarColor = Grey, kalendarSelector = KalendarSelector.Circle(defaultColor = Color.White, selectedColor = Azul800, todayColor = Azul500, eventTextColor = GreyV2, defaultTextColor = Smooth)),
                        onCurrentDayClick = { day, event ->
                            if(event != null){
                                if(event.eventName == "indisponível"){
                                    Data = hData
                                }
                            }else{
                                Data = day.toString()
                            }

                        }, errorMessage = {
                            //Handle the error if any
                        })
                }
                val btnAlterar = findViewById<Button>(R.id.btnAlterarData)
                btnAlterar.setOnClickListener {
                    mItemClickListener?.onListClick(Data)
                    dismiss()
                }

        }
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setLayout(width, height)
        }
    }

}