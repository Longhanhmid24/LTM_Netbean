/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chatapp_client;

import model.ChatMessage;
import model.MessageSendDTO;
import service.NetworkService;
import service.KeyService;
import service.CryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import org.java_websocket.client.WebSocketClient;
import java.net.http.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.security.PublicKey;
import java.nio.file.StandardOpenOption;
/**
 *
 * @author DELL
 */
public class IncomingCallDialog extends JDialog {
    public IncomingCallDialog(JFrame parent, String callerName, Runnable onAccept, Runnable onReject) {
        super(parent, "Cuộc gọi đến", true);
        setSize(320, 180);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10,10));

        JLabel lbl = new JLabel(callerName + " đang gọi...", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton btnAccept = new JButton("Bắt máy");
        btnAccept.setBackground(new Color(0, 180, 0));
        btnAccept.setForeground(Color.WHITE);
        btnAccept.addActionListener(e -> {
            onAccept.run();
            dispose();
        });

        JButton btnReject = new JButton("Từ chối");
        btnReject.setBackground(Color.RED);
        btnReject.setForeground(Color.WHITE);
        btnReject.addActionListener(e -> {
            onReject.run();
            dispose();
        });

        JPanel p = new JPanel(new GridLayout(1,2,10,0));
        p.add(btnAccept);
        p.add(btnReject);

        add(lbl, BorderLayout.CENTER);
        add(p, BorderLayout.SOUTH);
    }
}