package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

// Implementacion de prueba: imprime el correo en consola en lugar de usar un servidor SMTP real
@Service
public class ConsolaEmailService implements EmailService {
    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        System.out.println("[EMAIL] Para: " + destinatario + " | Asunto: " + asunto + "\n" + cuerpo);
    }
}
