package com.example.appmobile

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class resetFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var rootView : View = inflater.inflate(R.layout.fragment_reset, container, false)
        val btnEnviar = rootView.findViewById<Button>(R.id.btnEnviar)
        val PaginaReset = rootView.findViewById<FrameLayout>(R.id.PaginaReset)
        /* Envio de informações para o reset da palavra-passe */
        btnEnviar.setOnClickListener {
            val email = rootView.findViewById<EditText>(R.id.txtEmail).text.toString()
            val url = appSettings.URLrecuperacao
            val params = HashMap<String, String>()
            params["email"] = email
            val requestQueue = Volley.newRequestQueue(context)
            val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
                alertaSuccessFragment(requireActivity(), "Sucesso", getString(R.string.sucesso_recuperar_password))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            }, { error -> Log.i("resetFragment", error.toString()) })
            requestQueue.add(request)
        }
        return rootView
    }

}