package com.example.appmobile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException

class NotificacoesFragment : DialogFragment() {

    private lateinit var customAdapter: RecyclerAdapter
    lateinit var session : SessionManager

    private var listNotificacoes : MutableList<NotificacoesModel> = mutableListOf<NotificacoesModel>()

    override fun onResume() {
        super.onResume()
        getNotificacoes(object: VolleyCallBack {
            override fun onSuccess() {

                if(listNotificacoes.size == 0){
                    val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
                    val empty = view?.findViewById<LinearLayout>(R.id.empty)
                    empty?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.GONE
                }else{
                    val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
                    customAdapter = RecyclerAdapter(listNotificacoes)
                    val layoutManager = LinearLayoutManager(context)
                    recyclerView?.layoutManager = layoutManager
                    recyclerView?.adapter = customAdapter
                    customAdapter.notifyDataSetChanged()
                }

                val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START){
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder,
                    ): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        var gotCalled = false
                        val removedItem = listNotificacoes[viewHolder.adapterPosition]
                        val removedPosition = viewHolder.adapterPosition
                        val posList = customAdapter.getPosList(viewHolder.adapterPosition)

                        customAdapter.removeAt(viewHolder.adapterPosition)

                        Snackbar.make(viewHolder.itemView, getString(R.string.sucesso_notificacao_remover), Snackbar.LENGTH_SHORT)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(context?.let { ContextCompat.getColor(it, R.color.default_bg) }!!)
                            .setAction("Cancelar") {
                                customAdapter.addAt(removedPosition, removedItem)
                                gotCalled = true

                            }
                            .addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    super.onDismissed(transientBottomBar, event)
                                    if(!gotCalled)
                                        UpdateLido(posList)
                                }
                            })
                            .show()
                    }

                }
                val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
                val itemTouchHelper = ItemTouchHelper(swipeHandler)
                itemTouchHelper.attachToRecyclerView(recyclerView)
            }
            override fun onFailure(error: String) {
                val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
                val empty = view?.findViewById<LinearLayout>(R.id.empty)
                empty?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView : View = inflater.inflate(R.layout.fragment_notificacoes, container, false)

        /* Verificar Session */
        session = context?.let { SessionManager(it) }!!
        session.checkLogin()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val recyclerView: RecyclerView = rootView.findViewById(R.id.recyclerView)
        val adapter = RecyclerAdapter(listNotificacoes)
        recyclerView.adapter = adapter;
        /* Remover separados do Recycler */
        val itemDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(context?.let { ContextCompat.getDrawable(it, R.drawable.divider) }!!)

        val btnClose = rootView.findViewById<ImageButton>(R.id.closeFragment)
        btnClose.setOnClickListener {
            dialog?.dismiss()
        }
        return rootView
    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    private fun getNotificacoes(callback: VolleyCallBack?) {
        val url = appSettings.URLnotificacoes + session.getUser()
        listNotificacoes.clear()
        val requestQueue = Volley.newRequestQueue(context)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                //val nnotificacao: Int, val ntipo: Int, val titulo: String, val descricao: String, val datahora: String, val lido: Int, val permanencia: Int
                val nnotificacao = distr.getInt("nnotificacao")
                val ntipo = distr.getInt("ntipo")
                val titulo = distr.getString("titulo")
                val descricao = distr.getString("descricao")
                val datahora = distr.getString("datahora")
                val lido = distr.getInt("lido")
                val permanencia = distr.getInt("permanencia")
                if(lido == 0)
                    listNotificacoes.add(NotificacoesModel(nnotificacao, ntipo, titulo, descricao, datahora, lido, permanencia))

            }
            callback?.onSuccess()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    fun UpdateLido(n : Int){
        val requestQueue = Volley.newRequestQueue(context)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLdeleteNotificacao + n,
            Response.Listener { response ->
                if(!response.toBoolean()){
                    alertaDangerFragment(requireActivity(), "Atenção", getString(R.string.erro_geral))
                }
            },
            Response.ErrorListener { error ->
                Log.i("Fragment", error.toString())
                alertaDangerFragment(requireActivity(), "Atenção", getString(R.string.erro_geral))
            }) {
        }
        requestQueue.add(strRequest)

    }

}