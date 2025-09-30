// CanteenOrderSystem.java
// This file contains all classes for the Canteen Order System.
// For proper Java project structure, each public class should ideally be in its own .java file.

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;


// --- Superbase.java ---
class Superbase {
    public String tableName;
    public String tableId;
    public String superbaseName;

    public Superbase(String tableName, String tableId, String superbaseName) {
        this.tableName = tableName;
        this.tableId = tableId;
        this.superbaseName = superbaseName;
    }
}

// --- Order.java ---
class Order {
    private static int nextToken = 1;

    private String itemName;
    private int quantity;
    private float price;
    private int token;
    private LocalDateTime orderTime;
    private boolean isComplete;

    public Order(String itemName, int quantity, float price) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.token = nextToken++;
        this.orderTime = LocalDateTime.now();
        this.isComplete = false;
    }

    // Getters
    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }

    public int getToken() {
        return token;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public String getFormattedOrderTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return orderTime.format(formatter);
    }

    public boolean isComplete() {
        return isComplete;
    }

    public float getTotalPrice() {
        return quantity * price;
    }

    // Setter for completion status
    public void setComplete(boolean complete) {
        isComplete = complete;
    }
}

// --- OrderSystemLogic.java ---
class OrderSystemLogic {
    private Superbase superbase;
    private List<Order> orders;
    private List<OrderUpdateListener> listeners;

    public OrderSystemLogic(Superbase superbase) {
        this.superbase = superbase;
        this.orders = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public void addOrder(Order order) {
        orders.add(order);
        System.out.println("Order added: Token #" + order.getToken() + " - " + order.getItemName());
        notifyOrderAdded(order);
    }

    public void markOrderAsComplete(int token) {
        Optional<Order> orderToComplete = orders.stream()
                                            .filter(order -> order.getToken() == token)
                                            .findFirst();

        orderToComplete.ifPresent(order -> {
            order.setComplete(true);
            System.out.println("Order marked complete: Token #" + order.getToken());
            notifyOrderStatusChanged(order);
        });
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders);
    }

    public List<Order> getPendingOrders() {
        List<Order> pending = new ArrayList<>();
        for (Order order : orders) {
            if (!order.isComplete()) {
                pending.add(order);
            }
        }
        return pending;
    }

    // Listener Management
    public void addOrderUpdateListener(OrderUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeOrderUpdateListener(OrderUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyOrderAdded(Order order) {
        for (OrderUpdateListener listener : listeners) {
            listener.orderAdded(order);
        }
    }

    private void notifyOrderStatusChanged(Order order) {
        for (OrderUpdateListener listener : listeners) {
            listener.orderStatusChanged(order);
        }
    }

    // Interface for UI components to listen for changes
    public interface OrderUpdateListener {
        void orderAdded(Order order);
        void orderStatusChanged(Order order);
    }
}

// --- AdminTokenSlip.java ---
class AdminTokenSlip extends JFrame {
    private JPanel detailsPanel;

    public AdminTokenSlip() {
        setTitle("Order Token Slip");
        setSize(300, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void displayTokenSlip(Order order) {
        detailsPanel.removeAll();

        JLabel titleLabel = new JLabel("--- Canteen Order Slip ---");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsPanel.add(titleLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        addDetailRow("Token Number:", String.valueOf(order.getToken()));
        addDetailRow("Item Name:", order.getItemName());
        addDetailRow("Quantity:", String.valueOf(order.getQuantity()));
        addDetailRow("Price per Item:", String.format("$%.2f", order.getPrice()));
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel totalLabel = new JLabel(String.format("Total: $%.2f", order.getTotalPrice()));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsPanel.add(totalLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        addDetailRow("Order Time:", order.getFormattedOrderTime());
        addDetailRow("Status:", order.isComplete() ? "Completed" : "Pending");
        detailsPanel.add(Box.createVerticalGlue());

        detailsPanel.revalidate();
        detailsPanel.repaint();
        setVisible(true);
    }

    private void addDetailRow(String labelText, String valueText) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel value = new JLabel(valueText);
        value.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rowPanel.add(label);
        rowPanel.add(value);
        detailsPanel.add(rowPanel);
    }
}

// --- UI.java (Customer-facing) ---
class UI extends JFrame implements OrderSystemLogic.OrderUpdateListener {
    private Superbase superbase;
    private OrderSystemLogic orderSystemLogic;
    private AdminTokenSlip adminTokenSlip;

    private JTextField itemNameField;
    private JSpinner quantitySpinner;
    private JTextField priceField;
    private JButton placeOrderButton;
    private JLabel confirmationLabel;

    public UI(Superbase superbase, OrderSystemLogic orderSystemLogic) {
        this.superbase = superbase;
        this.orderSystemLogic = orderSystemLogic;
        this.orderSystemLogic.addOrderUpdateListener(this);
        this.adminTokenSlip = new AdminTokenSlip();

        setTitle("Canteen Order System - Customer");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        initComponents();
        layoutComponents();
        addEventHandlers();
    }

    private void initComponents() {
        itemNameField = new JTextField(20);
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        priceField = new JTextField(10);
        placeOrderButton = new JButton("Place Order");
        confirmationLabel = new JLabel("Enter your order details.");
        confirmationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        confirmationLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
    }

    private void layoutComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        formPanel.add(itemNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(quantitySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Price ($):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        formPanel.add(priceField, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(placeOrderButton);
        add(buttonPanel, BorderLayout.SOUTH);

        add(confirmationLabel, BorderLayout.NORTH);
    }

    private void addEventHandlers() {
        placeOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placeOrder();
            }
        });
    }

    public void placeOrder() {
        String itemName = itemNameField.getText().trim();
        int quantity = (Integer) quantitySpinner.getValue();
        float price;

        if (itemName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (quantity <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            price = Float.parseFloat(priceField.getText().trim());
            if (price < 0) {
                JOptionPane.showMessageDialog(this, "Price cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid price format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Order newOrder = new Order(itemName, quantity, price);
        orderSystemLogic.addOrder(newOrder);

        itemNameField.setText("");
        quantitySpinner.setValue(1);
        priceField.setText("");

        confirmationLabel.setText("Order placed - Token #" + newOrder.getToken());

        SwingUtilities.invokeLater(() -> adminTokenSlip.displayTokenSlip(newOrder));
    }

    @Override
    public void orderAdded(Order order) {
        // Not used directly in this customer UI for its own orders as it reacts immediately.
    }

    @Override
    public void orderStatusChanged(Order order) {
        // Not implemented to track past order status for customer UI.
    }
}

// --- AdminUI.java ---
class AdminUI extends JFrame implements OrderSystemLogic.OrderUpdateListener {
    private Superbase superbase;
    private OrderSystemLogic orderSystemLogic;

    private JTable orderTable;
    private DefaultTableModel tableModel;
    private JButton markCompleteButton;
    private JButton refreshButton;

    public AdminUI(Superbase superbase, OrderSystemLogic orderSystemLogic) {
        this.superbase = superbase;
        this.orderSystemLogic = orderSystemLogic;
        this.orderSystemLogic.addOrderUpdateListener(this);

        setTitle("Canteen Order System - Admin");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        initComponents();
        layoutComponents();
        addEventHandlers();
        refreshOrderTable();
    }

    private void initComponents() {
        String[] columnNames = {"Token", "Item", "Quantity", "Price", "Total", "Status", "Time"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(tableModel);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        markCompleteButton = new JButton("Mark as Complete");
        refreshButton = new JButton("Refresh Orders");
    }

    private void layoutComponents() {
        JScrollPane scrollPane = new JScrollPane(orderTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(markCompleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        JLabel headerLabel = new JLabel("Pending Canteen Orders", SwingConstants.CENTER);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(headerLabel, BorderLayout.NORTH);
    }

    private void addEventHandlers() {
        markCompleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = orderTable.getSelectedRow();
                if (selectedRow != -1) {
                    int token = (int) tableModel.getValueAt(selectedRow, 0);
                    orderSystemLogic.markOrderAsComplete(token);
                } else {
                    JOptionPane.showMessageDialog(AdminUI.this, "Please select an order to mark complete.", "No Order Selected", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshOrderTable();
            }
        });
    }

    private void refreshOrderTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            List<Order> pendingOrders = orderSystemLogic.getPendingOrders();
            for (Order order : pendingOrders) {
                Vector<Object> rowData = new Vector<>();
                rowData.add(order.getToken());
                rowData.add(order.getItemName());
                rowData.add(order.getQuantity());
                rowData.add(String.format("%.2f", order.getPrice()));
                rowData.add(String.format("%.2f", order.getTotalPrice()));
                rowData.add(order.isComplete() ? "Completed" : "Pending");
                rowData.add(order.getFormattedOrderTime());
                tableModel.addRow(rowData);
            }
        });
    }

    @Override
    public void orderAdded(Order order) {
        if (!order.isComplete()) {
            SwingUtilities.invokeLater(() -> {
                Vector<Object> rowData = new Vector<>();
                rowData.add(order.getToken());
                rowData.add(order.getItemName());
                rowData.add(order.getQuantity());
                rowData.add(String.format("%.2f", order.getPrice()));
                rowData.add(String.format("%.2f", order.getTotalPrice()));
                rowData.add("Pending");
                rowData.add(order.getFormattedOrderTime());
                tableModel.addRow(rowData);
            });
        }
    }

    @Override
    public void orderStatusChanged(Order order) {
        refreshOrderTable();
    }
}

// --- Main Application Entry Point ---
// This class contains the main method to run both UIs.
public class CanteenOrderSystem { // Changed from UI to CanteenOrderSystem to hold main
    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            Superbase base = new Superbase("CanteenOrders", "OrderTable", "CanteenSystem");
            OrderSystemLogic logic = new OrderSystemLogic(base);

            // Create and show the customer UI
            UI customerUI = new UI(base, logic);
            customerUI.setVisible(true);

            // Create and show the AdminUI
            AdminUI adminUI = new AdminUI(base, logic);
            adminUI.setVisible(true);
        });
    }
}