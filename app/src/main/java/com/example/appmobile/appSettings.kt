package com.example.appmobile


public class appSettings {

    companion object {

        private val ipAddress = "https://softinsa-backend.herokuapp.com/"
        //private val ipAddress = "http://192.168.1.130:24023/"

        val Abertura = "09:00"
        val Fecho = "20:00"

        val callBackend = ipAddress
        val URL_estabelecimentos_disponiveis = ipAddress + "estabelecimentos/disponiveis"
        val URLuserInfo = ipAddress + "users/infos/"
        val URLsalasEstabelecimento = ipAddress + "salas/estabelecimento/"
        val URLvalidarReserva = ipAddress + "reservas/validar/"
        val URLallReservas = ipAddress + "reservas/list/sala/"
        val URLaddReserva = ipAddress + "reservas/add"
        val URLunionReservas = ipAddress + "reservas/union/"
        val URLuploadImage = ipAddress + "uploads/upload/"
        val URLfindImage = ipAddress + "uploads/image/"
        val URLupdateNotify = ipAddress + "users/notify/"
        val URLupdateNome = ipAddress + "users/nome/"
        val URLupdateCargo = ipAddress + "users/cargo/"
        val URLnotificacoes = ipAddress + "notificacoes/user/"
        val URLdeleteNotificacao = ipAddress + "notificacoes/update/"
        val URLgetSala = ipAddress + "salas/get/"
        val URLreservasUser = ipAddress + "reservas/get/"
        val URLdeleteReserva = ipAddress + "reservas/delete/"
        val URLterminarReserva = ipAddress + "reservas/terminar/"
        val URLgetbypk = ipAddress + "reservas/getbypk/"
        val URL_salas_disponiveis = ipAddress + "algoritmos/salas"
        val URLeditReservaSala = ipAddress + "algoritmos/editsala/"
        val URLdatasEdit = ipAddress + "algoritmos/datas"
        val URLeditReservaData = ipAddress + "algoritmos/editdata/"
        val URLextenderReserva = ipAddress + "algoritmos/horas"
        val URLeditHoraReserva = ipAddress + "algoritmos/edithora/"
        val URLalterarEstadoSala = ipAddress + "algoritmos/estado/sala/"
        val URLvalidarReservaEdit = ipAddress + "reservas/validar_edit/"
        val URLeditarReservaHoras = ipAddress + "algoritmos/editar_horas"
        val URL_pesquisa_data_hora = ipAddress + "reservas/pesquisa/data"
        val URLestadoReserva = ipAddress + "algoritmos/estado_reserva"
        val URLupdateToken = ipAddress + "users/update/token"
        val URLloginEncryptado = ipAddress + "users/login"
        val URLchangePassword = ipAddress + "users/edit/forcepassword"
        val URLreservasHoje = ipAddress + "reservas/hoje/get/"
        val URLupdateEstado = ipAddress + "salas/update/estado"
        val URLrecuperacao = ipAddress + "send/email/recuperacao"
        val URL_imagem_sala = ipAddress + "uploads/sala/image/"
        val URL_validar_sobreposicao = ipAddress + "reservas/inserir/validar"
        val URL_validar_sobreposicao_edit = ipAddress + "reservas/alterar/validar/"
        val URL_minhas_reservas = ipAddress + "reservas/minhas/"
        val URL_edit_password = ipAddress + "users/edit/password"
        val URL_list_pedidos = ipAddress + "pedidos/list/"
        val URL_list_tipos = ipAddress + "pedidos/tipo/"
        val URL_finalizar_pedido = ipAddress + "pedidos/finish"
    }

}