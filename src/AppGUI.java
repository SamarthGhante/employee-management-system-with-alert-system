import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class AppGUI extends JFrame {
    private JTextField idField;
    private JTextField nameField;
    private JTextField salaryField;
    private JButton saveButton;
    private JButton deleteButton;
    private JButton displayButton;

    private String dbURL = "jdbc:mysql://localhost:3306/employee_db";
    private String username = "root";
    private String password = "root";
    private Connection conn;

    public AppGUI() {
        initializeUI();
        connectToDatabase();
    }

    private void initializeUI() {
        setTitle("Employee Database");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2));

        JLabel idLabel = new JLabel("Employee ID:");
        idField = new JTextField();

        JLabel nameLabel = new JLabel("Employee Name:");
        nameField = new JTextField();

        JLabel salaryLabel = new JLabel("Employee Salary:");
        salaryField = new JTextField();

        saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveEmployee();
            }
        });

        displayButton = new JButton("Display");
        displayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayEmployees();
            }
        });

        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteEmployee();
            }
        });

        // Adding Components to the screen
        panel.add(idLabel);
        panel.add(idField);
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(salaryLabel);
        panel.add(salaryField);
        panel.add(new JLabel());
        panel.add(saveButton);
        panel.add(displayButton);
        panel.add(deleteButton);

        add(panel);
    }

    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection(dbURL, username, password);
            if (conn != null) {
                System.out.println("Connected to database");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveEmployee() {
        int id = Integer.parseInt(idField.getText());
        String name = nameField.getText();
        int salary = Integer.parseInt(salaryField.getText());

        try {
            String sql = "INSERT INTO emp_info (employeeID, employeeName, employeeSalary) VALUES (?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.setInt(3, salary);
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(this, "Employee saved successfully!");

                // Send a POST request to the webhook URL
                String webhookURL = "https://webhook.samark.pp.ua/system-Alert";
                String message = "New employee added: ID-" + id + " - " + name + " - " + salary;
                sendWebhookRequest(webhookURL, message);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteEmployee() {
        String idString = JOptionPane.showInputDialog(this, "Enter Employee ID:");
        if (idString != null) {
            try {
                int id = Integer.parseInt(idString);
                String sql = "DELETE FROM emp_info WHERE employeeID=?";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setInt(1, id);
                int rowsDeleted = statement.executeUpdate();
                if (rowsDeleted > 0) {
                    JOptionPane.showMessageDialog(this, "Employee deleted successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "No employee found with the given ID.");
                }
            } catch (NumberFormatException | SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void displayEmployees() {
        try {
            String sql = "SELECT * FROM emp_info";
            Statement stmt = conn.createStatement();
            ResultSet result = stmt.executeQuery(sql);

            StringBuilder output = new StringBuilder();
            int count = 0;
            while (result.next()) {
                int eid = result.getInt(1);
                String ename = result.getString(2);
                int eage = result.getInt("employeeSalary");
                output.append(String.format("Employee #%d: ID-%d - %s - %d\n", ++count, eid, ename, eage));
            }

            if (count > 0) {
                JOptionPane.showMessageDialog(this, output.toString());
            } else {
                JOptionPane.showMessageDialog(this, "No employees found.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void sendWebhookRequest(String url, String message) {
        try {
            URL webhookURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) webhookURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Create the JSON payload
            String payload = "{\"message\": \"" + message + "\"}";

            // Send the request
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload.getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Webhook request sent successfully");
            } else {
                System.out.println("Failed to send webhook request. Response code: " + responseCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new AppGUI().setVisible(true);
            }
        });
    }
}
