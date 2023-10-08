package com.example.appmobile

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityOpcoesBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.himanshoe.kalendar.common.data.KalendarEvent
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import java.io.File
import java.io.IOException

class Opcoes : AppCompatActivity() {

    lateinit var session : SessionManager
    private lateinit var binding: ActivityOpcoesBinding
    private var imageData: ByteArray? = null
    private var selectedImageUri: Uri? = null

    companion object {
        private const val IMAGE_PICK_CODE = 999
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcoesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Para remover um sombrado atraz do menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.background = null
        /* Desativar o icon escondido que alinha o menu */
        bottomNavigationView.menu.getItem(2).isEnabled = false

        /* Executar botao QRCODE */
        val btnQrcode = findViewById<FloatingActionButton>(R.id.fab)
        btnQrcode.setOnClickListener {
            var i = Intent(applicationContext, QRcodeActivity::class.java)
            startActivity(i)
        }

        /* Botoes do Menu */
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    val iHome = Intent(applicationContext, Home::class.java)
                    startActivity(iHome)
                }
                R.id.page_2 -> {
                    val iPesquisa = Intent(applicationContext, pesquisaSalas::class.java)
                    startActivity(iPesquisa)
                }
                R.id.page_3 -> {
                    val iMinhas = Intent(applicationContext, MinhasReservas::class.java)
                    startActivity(iMinhas)
                }
                R.id.page_4 -> {
                    val Opcoes = Intent(applicationContext, Opcoes::class.java)
                    startActivity(Opcoes)
                }
            }
            return@setOnItemSelectedListener true
        }

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener {menuItem ->
            when (menuItem.itemId) {
                R.id.notify -> {
                    var dialog = NotificacoesFragment()
                    dialog.show(supportFragmentManager, "TAG")
                    true
                }
                else -> false
            }

        }

        try{
            var user: HashMap<String, String> = session.getUserDetails()
            var username = user.get(SessionManager.KEY_USERNAME)
            var cargo = user.get(SessionManager.KEY_CARGO)

            var txtUser = findViewById<TextView>(R.id.txtUser)
            var txtCargo = findViewById<TextView>(R.id.txtCargo)
            txtUser.text = username
            txtCargo.text = cargo
        }catch (e:Exception){
            Log.e("Session", "Não foi possivel obter os dados na sessao")
        }
        var getImg = session.getImagem()
        if(getImg == "" || getImg.isEmpty() || getImg.isBlank())
            getImg = "default.jpg"

        Picasso.get().load(appSettings.URLfindImage + getImg).resize(50, 50)
            .centerCrop()
            .placeholder(R.drawable.user)
            .error(R.drawable.user)
            .into(binding.imagePerfil)

        Log.i("Opcoes", appSettings.URLfindImage + getImg)
        val notifyStatus = session.getNotify()
        val swNotify = findViewById<SwitchMaterial>(R.id.swNotify)

        swNotify.isChecked = notifyStatus != 0
        binding.txtUser.text = session.getNome()
        binding.txtCargo.text = session.getCargo()

        /* Desativar/Ativar notificações */
        swNotify.setOnClickListener {
            if(session.getNotify() == 0)
                session.setNotify(1)
            else session.setNotify(0)

            val requestQueue = Volley.newRequestQueue(this)
            val strRequest: StringRequest = object : StringRequest(
                Method.POST, appSettings.URLupdateNotify + session.getUser(),
                Response.Listener { response ->
                    if(response.toBoolean()){
                        var ok = ""
                        if(session.getNotify() == 1) ok = getString(R.string.notificacoes_ativas)
                        else ok = getString(R.string.notificacoes_desativadas)
                        alertaSuccess(this@Opcoes, "Sucesso", ok)

                    }else{
                        alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_geral))
                    }
                },
                Response.ErrorListener { error ->
                   Log.i("Opcoes", error.toString())
                    alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_geral))
                }) {
                override fun getBodyContentType(): String {
                    return "application/json"
                }
                override fun getBody(): ByteArray {
                    val params = java.util.HashMap<String, String>()
                    params["nutilizador"] =  session.getUser().toString()
                    if(session.getNotify() == 0) params["estado"] = "0"
                    else params["estado"] = "1"
                    return JSONObject(params as Map<String, String>).toString().toByteArray()
                }
            }
            requestQueue.add(strRequest)

        }

        /* Alterar o nome de utilizador */
        val btnAlterarNome = findViewById<Button>(R.id.btnAlterarNome)
        btnAlterarNome.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_nome, null)
            val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
            val editNome = dialogView.findViewById<EditText>(R.id.editNome)
            editNome.setText(session.getNome())
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnContinuar.setOnClickListener {
                if(editNome.text.toString() != ""){
                    session.setNome(editNome.text.toString())
                    binding.txtUser.text = editNome.text.toString()
                    customDialog.dismiss()
                    val requestQueue = Volley.newRequestQueue(this)
                    val strRequest: StringRequest = object : StringRequest(
                        Method.POST, appSettings.URLupdateNome + session.getUser(),
                        Response.Listener { response ->
                            if(response.toBoolean()){
                                alertaSuccess(this@Opcoes, "Sucesso", getString(R.string.edit_utilizador_sucesso))
                            }else{
                                alertaDanger(this@Opcoes, "Atenção", getString(R.string.edit_utilizador_erro))
                            }
                        },
                        Response.ErrorListener { error ->
                            Log.i("Opcoes", error.toString())
                            alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_geral))
                        }) {
                        override fun getBodyContentType(): String {
                            return "application/json"
                        }
                        override fun getBody(): ByteArray {
                            val params = java.util.HashMap<String, String>()
                            params["nome"] =  editNome.text.toString()
                            return JSONObject(params as Map<String, String>).toString().toByteArray()
                        }
                    }
                    requestQueue.add(strRequest)
                }else{
                    alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_campo_vazio))
                }
            }
        }

        /* Alterar o cargo do utilizador */
        val btnAlterarCargo = findViewById<Button>(R.id.btnAlterarCargo)
        btnAlterarCargo.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_cargo, null)
            val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
            val editNome = dialogView.findViewById<EditText>(R.id.editNome)
            editNome.setText(session.getCargo())
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnContinuar.setOnClickListener {
                if(editNome.text.toString() != ""){
                    session.setCargo(editNome.text.toString())
                    binding.txtCargo.text = editNome.text.toString()
                    customDialog.dismiss()
                    val requestQueue = Volley.newRequestQueue(this)
                    val strRequest: StringRequest = object : StringRequest(
                        Method.POST, appSettings.URLupdateCargo + session.getUser(),
                        Response.Listener { response ->
                            if(response.toBoolean()){
                                alertaSuccess(this@Opcoes, "Sucesso", getString(R.string.edit_cargo_sucesso))
                            }else{
                                alertaDanger(this@Opcoes, "Atenção", getString(R.string.edit_cargo_erro))
                            }
                        },
                        Response.ErrorListener { error ->
                            Log.i("Opcoes", error.toString())
                            alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_geral))
                        }) {
                        override fun getBodyContentType(): String {
                            return "application/json"
                        }
                        override fun getBody(): ByteArray {
                            val params = java.util.HashMap<String, String>()
                            params["cargo"] =  editNome.text.toString()
                            return JSONObject(params as Map<String, String>).toString().toByteArray()
                        }
                    }
                    requestQueue.add(strRequest)
                }else{
                    alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_campo_vazio))
                }
            }
        }

        /* Alterar a palavra-passe */
        val btnAlterarPass = findViewById<Button>(R.id.btnAlterarPass)
        btnAlterarPass.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
            val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnContinuar.setOnClickListener {

                val editAntiga = dialogView.findViewById<EditText>(R.id.editAntiga).text.toString()
                val editNova = dialogView.findViewById<EditText>(R.id.editNova).text.toString()
                val editNova2 = dialogView.findViewById<EditText>(R.id.editNova2).text.toString()

                if(editNova == editNova2){
                    if(editNova.length <= 6){
                        alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_password_curta))
                    }else alterarPassword(editAntiga, editNova)
                }else{
                    alertaDanger(this@Opcoes, "Atenção", getString(R.string.erro_password_correspondencia))
                }
                customDialog.dismiss()
            }
        }

        var logout = findViewById<Button>(R.id.btnLogout)
        logout.setOnClickListener {
            session.LogoutUser()
        }

    }

    interface VolleyCallBack {
        fun onSuccess(status : Boolean)
        fun onFailure(error: String)
    }

    private fun alterarPassword(antiga: String, nova: String){
        val url = appSettings.URL_edit_password
        val params = java.util.HashMap<String, String>()
        params["antiga"] = antiga
        params["nova"] = nova
        params["nutilizador"] = session.getUser().toString()

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){
                alertaSuccess(this@Opcoes, "Sucesso", getString(R.string.edit_password_sucesso))
            }else{
                alertaDanger(this@Opcoes, "Atenção", response.getString("message"))
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> Log.i("Edit", error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    @Throws(IOException::class)
    private fun createImageData(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.buffered()?.use {
            imageData = it.readBytes()
        }
    }

    fun ContentResolver.getFileName(fileUri: Uri): String {
        var name = ""
        val returnCursor = this.query(fileUri, null, null, null, null)
        if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            name = returnCursor.getString(nameIndex)
            returnCursor.close()
        }
        return name
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val uri = data?.data
            if (uri != null) {
                binding.imagePerfil.setImageURI(uri)
                selectedImageUri = uri
                createImageData(uri)
                uploadImage()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /* Carregar a imagem de perfil para a backend */
    private fun uploadImage() {
        imageData?: return
        val request = object : VolleyFileUploadRequest(
            Method.POST,
            appSettings.URLuploadImage + session.getUser(),
            Response.Listener { response ->
                getImagemVolley()
                alertaSuccess(this@Opcoes, "Sucesso", getString(R.string.edit_imagem_sucesso))
            },
            Response.ErrorListener {
                alertaDanger(this@Opcoes, "Atenção", getString(R.string.edit_imagem_erro))
            }
        ) {
            override fun getByteData(): MutableMap<String, FileDataPart> {
                val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
                var params = HashMap<String, FileDataPart>()
                params["file"] = FileDataPart(file.name, imageData!!, "image/jpeg")
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    fun mudarImg(v: View){
        launchGallery()
    }

    fun getImagemVolley(){

        val url = appSettings.URLuserInfo + session.getUser()
        val requestQueue = Volley.newRequestQueue(applicationContext)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
            val infos = response.getJSONObject("infos")
            session.setImagem(infos.getString("imagem"))

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> Log.i("Opcoes", error.toString()) })
        requestQueue.add(request)
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

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(4)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
        callBackend()
    }
}