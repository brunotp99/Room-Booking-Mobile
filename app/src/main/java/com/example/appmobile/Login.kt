package com.example.appmobile

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityLoginBinding
import com.example.appmobile.models.Estabelecimentos
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import com.tapadoo.alerter.Alerter
import org.json.JSONException
import org.json.JSONObject


class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    lateinit var topAppBar : MaterialToolbar
    lateinit var session : SessionManager

    private var listEstabelecimentos = ArrayList<Estabelecimentos>()

    var idEst = -1
    var idUser = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /* Verificar Sessao */
        session = SessionManager(this)
        if(session.isLoggedIn()){

            if(session.getTipo() == "outro"){
                var i: Intent = Intent(applicationContext, outrosActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }else{
                var i: Intent = Intent(applicationContext, Home::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }

        }

        callBackend()
        recuperarPassword()

        /* Função da Firebase para gerar tokens */
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Não foi póssivel gerar o token!", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            session.setTokenFirebase(token)
        })

        /* Obter Dados da DB */
        getLocais(object: VolleyCallBack {
            override fun onSuccess() {
                binding.progessBar.visibility = View.GONE
                val textField = findViewById<TextInputLayout>(R.id.textField)
                val items = ArrayList<String>()
                for(item in listEstabelecimentos){
                    if(item.estado != 0)
                        items.add(item.estabelecimento)
                }

                val adapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, items)
                (textField.editText as? AutoCompleteTextView)?.setAdapter(adapter)
            }

            override fun onFailure(error: String) {
                binding.progessBar.visibility = View.GONE
                Log.i("Login", error)
                binding.autoCompleteLocais.dismissDropDown()

                Alerter.create(this@Login)
                    .setTitle("Sem Conexão")
                    .setText("Carregue aqui para tentar novamente...")
                    .setIcon(R.drawable.ic_error)
                    .setIconColorFilter(0)
                    .setBackgroundColorRes(R.color.danger_bg)
                    .enableSwipeToDismiss()
                    .enableInfiniteDuration(true)
                    .setOnClickListener(View.OnClickListener {
                        val i = Intent(applicationContext, Login::class.java)
                        startActivity(i)
                    })
                    .show()
            }

        })

        /* Botao entrar na conta */
        binding.btnEntrar.setOnClickListener {
            val selectedValue = (binding.textField.editText as AutoCompleteTextView).text.toString()
            /* Obter ID do estabelecimento */
            for(item in listEstabelecimentos){
                if(item.estabelecimento == selectedValue)
                    idEst = item.nestabelecimento
            }
            if(idEst == -1){
                alertaDanger(this@Login, "Atenção", getString(R.string.erro_estabelecimento_vazio))
                return@setOnClickListener
            }

            val getUser = findViewById<TextInputLayout>(R.id.utilizador).editText?.text.toString()
            val getPass = findViewById<TextInputLayout>(R.id.password).editText?.text.toString()

            /* Verificar conteudo das EditText */
            if(getUser == "" || getUser.isEmpty() || getUser.isBlank() || getPass == "" || getPass.isEmpty() || getPass.isBlank()){
                alertaDanger(this@Login, "Atenção", getString(R.string.erro_campo_vazio))
                return@setOnClickListener
            }

            /* Validar o Acesso */
            validateLogin(object: VolleyCallBack {
                override fun onSuccess() {
                    Log.i("Login", "Logado com sucesso")
                }

                override fun onFailure(error: String) {
                    binding.progessBar.visibility = View.GONE
                    alertaDanger(this@Login, "Atenção", error)
                }

            }, getUser, getPass, idEst.toString())

        }

    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Obter estabelecimentos da backend */
    private fun getLocais(callback: VolleyCallBack?) {
        val url = appSettings.URL_estabelecimentos_disponiveis
        listEstabelecimentos.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                val nestabelecimento = distr.getInt("nestabelecimento")
                val estabelecimento = distr.getString("estabelecimento")
                val estado = distr.getInt("estado")
                val localidade = distr.getString("localidade")
                listEstabelecimentos.add(Estabelecimentos(nestabelecimento, estabelecimento, estado, localidade))

            }
            callback?.onSuccess()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Obter informações do utilizador logado */
    private fun getUsers() {
        val url = appSettings.URLuserInfo + idUser

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            val infos = response.getJSONObject("infos")
            val tipo = response.getString("tipo")

            if(infos.getInt("verifypassword") == 0){
                AlterarPassword(idUser, infos.getString("utilizador"), infos.getString("email"), idEst, infos.getString("cargo"), infos.getString("imagem"), infos.getInt("notify"))
            }else{
                if(tipo == "outro"){
                    session.createLoginSession(idUser, infos.getString("utilizador"), infos.getString("email"), idEst, infos.getString("cargo"), infos.getString("imagem"), infos.getInt("notify"), "outro")
                    var i = Intent(applicationContext, outrosActivity::class.java)
                    startActivity(i)
                    updateToken()
                }else if(tipo == "requisitante" || tipo == "gestor"){
                    session.createLoginSession(idUser, infos.getString("utilizador"), infos.getString("email"), idEst, infos.getString("cargo"), infos.getString("imagem"), infos.getInt("notify"), "requisitante")
                    var i = Intent(applicationContext, Home::class.java)
                    startActivity(i)
                    updateToken()
                }else{
                    alertaDanger(this@Login, "Atenção", "Desculpe, a sua conta não tem acesso a aplicação mobile.")
                }
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> Log.i("Teste", error.toString()) })
        requestQueue.add(request)
    }

    /* Função para alterar a palavra-passe do utilizador */
    fun AlterarPassword(id : Int, utilizador : String, email : String, estabelecimento: Int, cargo : String, imagem : String, notify : Int){
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_recuperacao, null)
        val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)

        dialog.setView(dialogView)
        dialog.setCancelable(true)
        val customDialog = dialog.create()
        customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customDialog.show()

        btnContinuar.setOnClickListener {

            val editNova = dialogView.findViewById<EditText>(R.id.editNova).text.toString()
            val editNova2 = dialogView.findViewById<EditText>(R.id.editNova2).text.toString()

            if (editNova == editNova2) {

                customDialog.dismiss()
                val requestQueue = Volley.newRequestQueue(this)
                val strRequest: StringRequest = object : StringRequest(
                    Method.POST, appSettings.URLchangePassword,
                    Response.Listener { response ->
                        if (response.toBoolean()) {
                            session.createLoginSession(idUser, utilizador, email, idEst, cargo, imagem, notify, "requisitante")
                            var i = Intent(applicationContext, Home::class.java)
                            startActivity(i)
                            updateToken()
                        } else {
                            alertaDanger(this@Login, "Atenção", getString(R.string.erro_geral))
                        }
                    },
                    Response.ErrorListener { error ->
                        Log.i("Login", error.toString())
                        alertaDanger(this@Login, "Atenção", getString(R.string.erro_geral))
                    }) {
                    override fun getBodyContentType(): String {
                        return "application/json"
                    }

                    override fun getBody(): ByteArray {
                        val params = java.util.HashMap<String, String>()
                        params["email"] = email
                        params["password"] = editNova
                        return JSONObject(params as Map<String, String>).toString().toByteArray()
                    }
                }
                requestQueue.add(strRequest)
            }
        }
    }

    /* Função para validar os dados de acesso do utilizador */
    private fun validateLogin(callback: VolleyCallBack?, user: String, pass: String, estabelecimento: String) {
        val url = appSettings.URLloginEncryptado
        binding.progessBar.visibility = View.VISIBLE
        val params = HashMap<String, String>()
        params["email"] = user
        params["password"] = pass
        params["estabelecimento"] = estabelecimento
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            binding.progessBar.visibility = View.GONE
            val getStatus = response.getBoolean("success")
            var message = "Desculpe, os dados inseridos não são válidos!"

            if(response.has("message")){
                val msg = response.getString("message")
                when(msg){
                    "vazio" -> message = "Por favor, preencha todos os campos!"
                    "utilizador_desativado" -> message = "Atenção, a sua conta foi desativada, por favor entre em contacto com um administrador."
                    "nao_associado" -> message = "Desculpe, não tem acesso ao estabelecimento selecionado."
                    "dados_invalidos" -> message = "Desculpe, os dados inseridos não são válidos!"
                    else -> message = "Desculpe, os dados inseridos não são válidos!"
                }
            }
            if(!getStatus){
                callback?.onFailure(message)
            } else{
                idUser = response.getInt("id")
                getUsers()
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure("Desculpe, atualmente não é póssivel iniciar a sessão."); })
        requestQueue.add(request)
    }

    /* Função para atualizar o token da Firebase para receber notificações */
    fun updateToken(){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLupdateToken,
            Response.Listener { response ->
                Log.i("Login", "Token atualizado")
            },
            Response.ErrorListener { error ->
                Log.i("Login", "Token não atualizado")
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                var params = HashMap<String, String>()
                params["nutilizador"] = idUser.toString()
                params["token"] = session.getTokenFirebase()
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    fun recuperarPassword(){
        binding.esqueciPass.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_recuperar, null)
            val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
            val editPassword = dialogView.findViewById<EditText>(R.id.editPassword)
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnContinuar.setOnClickListener {
                if(editPassword.text.toString() != "") {
                    customDialog.dismiss()

                    binding.progessBar.visibility = View.VISIBLE
                    val requestQueue = Volley.newRequestQueue(this)
                    val strRequest: StringRequest = object : StringRequest(
                        Method.POST, appSettings.URLrecuperacao,
                        Response.Listener { response ->
                            binding.progessBar.visibility = View.GONE
                            Log.i("Login", "Sucesso")
                            infoDialog(getString(R.string.sucesso_recuperar_password))
                        },
                        Response.ErrorListener { error ->
                            binding.progessBar.visibility = View.GONE
                            Log.i("Login", error.toString())
                            infoDialog(getString(R.string.erro_geral))
                        }) {
                        override fun getBodyContentType(): String {
                            return "application/json"
                        }

                        override fun getBody(): ByteArray {
                            val params = java.util.HashMap<String, String>()
                            params["email"] = editPassword.text.toString()
                            return JSONObject(params as Map<String, String>).toString()
                                .toByteArray()
                        }
                    }
                    strRequest.retryPolicy = DefaultRetryPolicy(
                        10000,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
                    requestQueue.add(strRequest)
                }else{
                    customDialog.dismiss()
                    infoDialog(getString(R.string.erro_campo_vazio))
                }
            }
        }
    }

    private fun infoDialog(message: String){
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_qrcode, null)
        val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
        val txtInfo = dialogView.findViewById<TextView>(R.id.txtInfo)
        txtInfo.text = message
        dialog.setView(dialogView)
        dialog.setCancelable(true)
        val customDialog = dialog.create()
        customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customDialog.show()
        btnContinuar.setOnClickListener {
            customDialog.dismiss()
        }
    }


    /* Verificar se a backend esta online, caso contrario mostrar aviso de erro */
    fun callBackend(){
        val url = appSettings.callBackend
        val requestQueue = Volley.newRequestQueue(applicationContext)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            val dialogView = layoutInflater.inflate(R.layout.dialog_connection, null)
            val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnVoltar.setOnClickListener {
                customDialog.dismiss()
            }
        })
        requestQueue.add(request)
    }

}