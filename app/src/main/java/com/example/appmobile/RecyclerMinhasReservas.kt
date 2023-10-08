package com.example.appmobile

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

internal class RecyclerMinhasReservas(private var listReservas: MutableList<minhasReservasModel> = mutableListOf<minhasReservasModel>()) :
    RecyclerView.Adapter<RecyclerMinhasReservas.MyViewHolder>() {


    internal interface MyCallback {
        fun onListClick(position: Int, status : Int, intervalo : Int)
        fun onEditClick(nreserva: String, nsala : Int, sala : String, data : String, inicio : String, fim : String, status : Int, intervalo : Int, imagem : String)
    }

    var mItemClickListener: MyCallback? = null
    fun setOnClickListener(click: MyCallback) {
        mItemClickListener = click
    }

    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.title)
        var horario: TextView = view.findViewById(R.id.horario)
        var emcurso: TextView = view.findViewById(R.id.emcurso)
        var info: ImageView = view.findViewById(R.id.info_button)
        var edit: ImageView = view.findViewById(R.id.edit_button)

    }
    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservas, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.title.text = "Reserva na " + listReservas[position].sala
        holder.horario.text = listReservas[position].horainicio + " - " + listReservas[position].fimreserva
        val current = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val formatted = current.format(formatter).toString()
        var status = 0

        if(listReservas[position].datareserva == formatted){
            if(listReservas[position].estadoreserva == 3){
                holder.emcurso.text = "Em Curso"
                holder.emcurso.getCompoundDrawables()[0]?.setTint(Color.parseColor("#FD3324"))
                holder.edit.setBackgroundResource(R.drawable.parar_reserva)
                status = 1
            }
        }

        holder.info.setOnClickListener(View.OnClickListener { v ->
            mItemClickListener?.onEditClick(listReservas[position].nreserva.toString(), listReservas[position].nsala, listReservas[position].sala, listReservas[position].datareserva, listReservas[position].horainicio, listReservas[position].horafim, status, listReservas[position].intervalo, listReservas[position].imagem)
        })
        holder.edit.setOnClickListener(View.OnClickListener { v ->
            mItemClickListener?.onListClick(position, status, listReservas[position].intervalo)
        })
    }
    override fun getItemCount(): Int {
        return listReservas.size
    }

    fun removeAt(position: Int) {
        listReservas.removeAt(position)
        notifyItemRemoved(position)
    }

}