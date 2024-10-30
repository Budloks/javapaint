import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Stack;

public class JavaPaint extends JFrame {

    private String shapeType = "Line";
    private Color drawColor = Color.BLACK;
    private Color fillColor = Color.WHITE;
    private Color canvasBackgroundColor = Color.WHITE;
    private int strokeWidth = 1;
    private Stack<ArrayList<ShapeData>> history = new Stack<>();
    private ArrayList<ShapeData> shapes = new ArrayList<>();
    private ShapeData tempShape = null;
    private Point startPoint, endPoint;

    private double scale = 1.0;
    private double minScale = 0.5, maxScale = 5.0;
    private Point canvasOrigin = new Point(0, 0);
    private boolean draggingCanvas = false;
    private Point lastMousePoint;

    private int canvasWidth = 800;
    private int canvasHeight = 600;

    private ArrayList<Point> polygonPoints = new ArrayList<>();
    private boolean fillShape = false; // To manage fill option

    public JavaPaint() {
        super("Java Paint with Live Drawing");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Drawing panel
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.scale(scale, scale);
                g2d.translate(canvasOrigin.x / scale, canvasOrigin.y / scale);

                g2d.setColor(canvasBackgroundColor);
                g2d.fillRect(0, 0, canvasWidth, canvasHeight);

                for (ShapeData shape : shapes) {
                    if (shape != null) {
                        shape.drawShape(g2d);
                    }
                }
                if (tempShape != null) {
                    tempShape.drawShape(g2d);
                }

                // Draw polygon points
                if (shapeType.equals("Polygon")) {
                    g2d.setColor(Color.RED);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    for (int i = 0; i < polygonPoints.size(); i++) {
                        Point p = polygonPoints.get(i);
                        g2d.fillOval(p.x - 4, p.y - 4, 8, 8);
                        g2d.drawString(String.valueOf(i + 1), p.x + 5, p.y - 5);
                    }
                }
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(canvasWidth, canvasHeight);
            }
        };
        InputMap inputMap = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = canvas.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!history.isEmpty()) {
                    history.pop();
                    shapes = history.isEmpty() ? new ArrayList<>() : new ArrayList<>(history.peek());
                    canvas.repaint();
                }
            }
        });
        canvas.setBackground(Color.DARK_GRAY);
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point clickPoint = new Point(
                        (int) ((e.getX() - canvasOrigin.x) / scale),
                        (int) ((e.getY() - canvasOrigin.y) / scale)
                );

                // Ensure the clickPoint is within the canvas bounds before drawing
                if (clickPoint.x >= 0 && clickPoint.x <= canvasWidth &&
                        clickPoint.y >= 0 && clickPoint.y <= canvasHeight) {
                    startPoint = clickPoint;
                }

                lastMousePoint = e.getPoint();

                if (SwingUtilities.isMiddleMouseButton(e)) {
                    draggingCanvas = true; // Pan the canvas
                } else if (shapeType.equals("Polygon") && SwingUtilities.isLeftMouseButton(e) && startPoint != null) {
                    polygonPoints.add(clickPoint);
                    canvas.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // When releasing after drawing
                if (SwingUtilities.isLeftMouseButton(e) && startPoint != null) {
                    endPoint = tempShape != null ? tempShape.getEnd() : startPoint;

                    if (!shapeType.equals("Polygon")) {
                        shapes.add(new ShapeData(startPoint, endPoint, shapeType, drawColor, fillShape ? fillColor : null, strokeWidth));
                        history.push(new ArrayList<>(shapes));
                        tempShape = null;
                    }

                    startPoint = null;
                    endPoint = null;
                    canvas.repaint();
                }

                draggingCanvas = false;
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingCanvas) {
                    // Pan the canvas in any direction
                    canvasOrigin.x += e.getX() - lastMousePoint.x;
                    canvasOrigin.y += e.getY() - lastMousePoint.y;
                    lastMousePoint = e.getPoint();
                } else if (SwingUtilities.isLeftMouseButton(e) && startPoint != null && !shapeType.equals("Polygon")) {
                    // Adjust endPoint relative to the panned and scaled canvas
                    endPoint = new Point(
                            (int) ((e.getX() - canvasOrigin.x) / scale),
                            (int) ((e.getY() - canvasOrigin.y) / scale)
                    );

                    // Ensure drawing is clamped to the canvas bounds
                    endPoint = new Point(
                            Math.max(0, Math.min(endPoint.x, canvasWidth)),
                            Math.max(0, Math.min(endPoint.y, canvasHeight))
                    );

                    // Create temporary shape while dragging
                    tempShape = new ShapeData(startPoint, endPoint, shapeType, drawColor, fillShape ? fillColor : null, strokeWidth);
                }
                canvas.repaint();
            }
        });

        canvas.addMouseWheelListener(e -> {
            double zoomFactor = 1.1;
            int notches = e.getWheelRotation();
            double newScale = scale;

            if (notches < 0) { // Zoom in
                newScale = Math.min(maxScale, scale * zoomFactor);
            } else { // Zoom out
                newScale = Math.max(minScale, scale / zoomFactor);
            }

            if (newScale != scale) {
                // Adjust canvasOrigin to zoom in/out centered at the mouse cursor
                Point mousePoint = e.getPoint();
                int zoomOriginX = (int) ((mousePoint.x - canvasOrigin.x) / scale);
                int zoomOriginY = (int) ((mousePoint.y - canvasOrigin.y) / scale);
                scale = newScale;

                canvasOrigin.x = mousePoint.x - (int) (zoomOriginX * scale);
                canvasOrigin.y = mousePoint.y - (int) (zoomOriginY * scale);

                canvas.repaint();
            }
        });


        add(canvas, BorderLayout.CENTER);

        // Tools panel
        JPanel toolsPanel = new JPanel();
        toolsPanel.setBackground(new Color(181, 181, 181));
        toolsPanel.setForeground(Color.WHITE);

        JButton donePolygonButton = new JButton("Done");
        donePolygonButton.setBackground(Color.DARK_GRAY);
        donePolygonButton.setForeground(Color.WHITE);
        donePolygonButton.setVisible(false);  // Initially hidden

        donePolygonButton.addActionListener(e -> {
            if (polygonPoints.size() > 2) {
                shapes.add(new ShapeData(new ArrayList<>(polygonPoints), drawColor, fillShape ? fillColor : null, strokeWidth));
                history.push(new ArrayList<>(shapes));
                polygonPoints.clear();
                canvas.repaint();
            }
        });
        toolsPanel.add(donePolygonButton);

        String[] shapesOptions = {"Line", "Rectangle", "Oval", "Polygon"};
        JComboBox<String> shapesCombo = new JComboBox<>(shapesOptions);
        shapesCombo.setBackground(Color.DARK_GRAY);
        shapesCombo.setForeground(Color.WHITE);
        shapesCombo.addActionListener(e -> {
            shapeType = (String) shapesCombo.getSelectedItem();
            if (shapeType.equals("Polygon")) {
                polygonPoints.clear(); // Clear points when switching to polygon
            }
            canvas.repaint();
        });
        toolsPanel.add(new JLabel("Shape:", JLabel.RIGHT));
        toolsPanel.add(shapesCombo);

        shapesCombo.addActionListener(e -> {
            shapeType = (String) shapesCombo.getSelectedItem();

            if (shapeType.equals("Polygon")) {
                polygonPoints.clear(); // Clear points when switching to polygon
                donePolygonButton.setVisible(true);  // Show the button
            } else {
                donePolygonButton.setVisible(false); // Hide the button for other shapes
            }

            canvas.repaint();
        });

        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(e -> drawColor = JColorChooser.showDialog(null, "Choose Draw Color", drawColor));
        colorButton.setBackground(Color.DARK_GRAY);
        colorButton.setForeground(Color.WHITE);
        toolsPanel.add(colorButton);

        JButton fillColorButton = new JButton("Choose Fill Color");
        fillColorButton.addActionListener(e -> fillColor = JColorChooser.showDialog(null, "Choose Fill Color", fillColor));
        fillColorButton.setBackground(Color.DARK_GRAY);
        fillColorButton.setForeground(Color.WHITE);
        toolsPanel.add(fillColorButton);

        JButton backgroundColorButton = new JButton("Change Canvas Color");
        backgroundColorButton.setBackground(Color.DARK_GRAY);
        backgroundColorButton.setForeground(Color.WHITE);
        backgroundColorButton.addActionListener(e -> {
            Color newBackgroundColor = JColorChooser.showDialog(null, "Choose Canvas Background Color", canvasBackgroundColor);
            if (newBackgroundColor != null) {
                canvasBackgroundColor = newBackgroundColor;
                canvas.repaint();
            }
        });
        toolsPanel.add(backgroundColorButton);

        JCheckBox fillCheckBox = new JCheckBox("Fill Shape");
        fillCheckBox.setBackground(Color.DARK_GRAY);
        fillCheckBox.setForeground(Color.WHITE);
        fillCheckBox.addActionListener(e -> fillShape = fillCheckBox.isSelected());
        toolsPanel.add(fillCheckBox);

        JSpinner strokeWidthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        strokeWidthSpinner.addChangeListener(e -> strokeWidth = (Integer) strokeWidthSpinner.getValue());
        toolsPanel.add(new JLabel("Stroke Width:", JLabel.RIGHT));
        toolsPanel.add(strokeWidthSpinner);

        JTextField widthField = new JTextField("800", 5);
        JTextField heightField = new JTextField("600", 5);
        JButton setSizeButton = new JButton("Set Canvas Size");
        setSizeButton.setBackground(Color.DARK_GRAY);
        setSizeButton.setForeground(Color.WHITE);
        setSizeButton.addActionListener(e -> {
            canvasWidth = Integer.parseInt(widthField.getText());
            canvasHeight = Integer.parseInt(heightField.getText());
            canvas.setPreferredSize(new Dimension(canvasWidth, canvasHeight));
            canvas.revalidate();
            canvas.repaint();
        });
        toolsPanel.add(new JLabel("Width:"));
        toolsPanel.add(widthField);
        toolsPanel.add(new JLabel("Height:"));
        toolsPanel.add(heightField);
        toolsPanel.add(setSizeButton);

        JButton undoButton = new JButton("Undo (Ctrl+Z)");
        undoButton.addActionListener(e -> {
            if (!history.isEmpty()) {
                history.pop();
                shapes = history.isEmpty() ? new ArrayList<>() : new ArrayList<>(history.peek());
                canvas.repaint();
            }
        });
        undoButton.setBackground(Color.DARK_GRAY);
        undoButton.setForeground(Color.WHITE);
        toolsPanel.add(undoButton);

        JButton generateButton = new JButton("Generate Code");
        generateButton.addActionListener(e -> showGeneratedCode());
        generateButton.setBackground(Color.DARK_GRAY);
        generateButton.setForeground(Color.WHITE);
        toolsPanel.add(generateButton);

        add(toolsPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private boolean isMouseInCanvas(MouseEvent e) {
        return e.getX() >= 0 && e.getX() <= canvasWidth &&
                e.getY() >= 0 && e.getY() <= canvasHeight;
    }

    private String drawColorToString(Color color) {
        return String.format("new Color(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void showGeneratedCode() {
        StringBuilder code = new StringBuilder();
        code.append("import java.awt.*;\n");
        code.append("import javax.swing.*;\n\n");
        code.append("public class GeneratedDrawing extends JPanel {\n");

        // Add the background color initialization
        code.append("    public GeneratedDrawing() {\n");
        code.append("        setBackground(").append(drawColorToString(canvasBackgroundColor)).append(");\n");
        code.append("    }\n\n");

        code.append("    @Override\n");
        code.append("    protected void paintComponent(Graphics g) {\n");
        code.append("        super.paintComponent(g);\n");
        code.append("        Graphics2D g2d = (Graphics2D) g;\n");

        // Append the shape-drawing code for each shape
        for (ShapeData shape : shapes) {
            shape.generateCode(code);
        }

        // Close the paintComponent method and add the main method with canvas size and non-resizable window
        code.append("    }\n\n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        JFrame frame = new JFrame(\"Generated Drawing\");\n");
        code.append("        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n");
        code.append("        frame.add(new GeneratedDrawing());\n");
        code.append("        frame.setSize(").append(canvasWidth).append(", ").append(canvasHeight).append(");\n");
        code.append("        frame.setResizable(false);\n");
        code.append("        frame.setVisible(true);\n");
        code.append("    }\n");
        code.append("}\n");

        // Display the generated code
        JTextArea textArea = new JTextArea(code.toString());
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "Generated Code", JOptionPane.INFORMATION_MESSAGE);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(JavaPaint::new);
    }
}

class ShapeData {
    private Point start, end;
    private ArrayList<Point> polygonPoints;
    private Color drawColor, fillColor;
    private int strokeWidth;
    private String type;

    public ShapeData(Point start, Point end, String type, Color drawColor, Color fillColor, int strokeWidth) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.drawColor = drawColor != null ? drawColor : Color.BLACK; // Set default if null
        this.fillColor = fillColor; // You might want to add a check here too
        this.strokeWidth = strokeWidth;
    }


    public ShapeData(ArrayList<Point> polygonPoints, Color drawColor, Color fillColor, int strokeWidth) {
        this.polygonPoints = polygonPoints;
        this.drawColor = drawColor;
        this.fillColor = fillColor;
        this.strokeWidth = strokeWidth;
        this.type = "Polygon";
    }

    public Point getEnd() {
        return end;
    }

    public void drawShape(Graphics2D g2d) {
        g2d.setColor(drawColor);
        g2d.setStroke(new BasicStroke(strokeWidth));

        if (type.equals("Line")) {
            g2d.drawLine(start.x, start.y, end.x, end.y);
        } else if (type.equals("Rectangle")) {
            g2d.drawRect(Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.abs(start.x - end.x), Math.abs(start.y - end.y));
            if (fillColor != null) {
                g2d.setColor(fillColor);
                g2d.fillRect(Math.min(start.x, end.x), Math.min(start.y, end.y),
                        Math.abs(start.x - end.x), Math.abs(start.y - end.y));
                g2d.setColor(drawColor); // Reset color for border
            }
        } else if (type.equals("Oval")) {
            g2d.drawOval(Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.abs(start.x - end.x), Math.abs(start.y - end.y));
            if (fillColor != null) {
                g2d.setColor(fillColor);
                g2d.fillOval(Math.min(start.x, end.x), Math.min(start.y, end.y),
                        Math.abs(start.x - end.x), Math.abs(start.y - end.y));
                g2d.setColor(drawColor); // Reset color for border
            }
        } else if (type.equals("Polygon") && polygonPoints != null) {
            if (polygonPoints != null && !polygonPoints.isEmpty()) {
                int[] xPoints = new int[polygonPoints.size()];
                int[] yPoints = new int[polygonPoints.size()];
                for (int i = 0; i < polygonPoints.size(); i++) {
                    xPoints[i] = polygonPoints.get(i).x;
                    yPoints[i] = polygonPoints.get(i).y;
                }
                g2d.drawPolygon(xPoints, yPoints, polygonPoints.size());
                if (fillColor != null) {
                    g2d.setColor(fillColor);
                    g2d.fillPolygon(xPoints, yPoints, polygonPoints.size());
                }
            }
        }
    }

    private String drawColorToString(Color color) {
        if (color == null) {
            // Handle the null case, either by returning a default value or throwing an exception
            return "new Color(0, 0, 0)"; // Returning black as a default color
        }
        return String.format("new Color(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }



    public void generateCode(StringBuilder code) {
        // Track the previous color and stroke width
        String lastDrawColor = "null";
        String lastFillColor = "null";
        int lastStrokeWidth = -1;

        // Prepare shape drawing commands
        StringBuilder shapeDrawCall = new StringBuilder();
        StringBuilder xPoints = new StringBuilder();
        StringBuilder yPoints = new StringBuilder();

        if ("Line".equals(type)) {
            shapeDrawCall.append(String.format("g2d.drawLine(%d, %d, %d, %d);\n", start.x, start.y, end.x, end.y));
        } else if ("Rectangle".equals(type)) {
            shapeDrawCall.append(String.format("g2d.drawRect(%d, %d, %d, %d);\n",
                    Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
        } else if ("Oval".equals(type)) {
            shapeDrawCall.append(String.format("g2d.drawOval(%d, %d, %d, %d);\n",
                    Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
        } else if ("Polygon".equals(type)) {
            // Generate x and y points without arrays
            for (int i = 0; i < polygonPoints.size(); i++) {
                Point p = polygonPoints.get(i);
                xPoints.append(p.x);
                yPoints.append(p.y);
                if (i < polygonPoints.size() - 1) {
                    xPoints.append(", ");
                    yPoints.append(", ");
                }
            }

            shapeDrawCall.append(String.format("g2d.drawPolygon(new int[] {%s}, new int[] {%s}, %d);\n",
                    xPoints.toString(), yPoints.toString(), polygonPoints.size()));
        }

        // Only set the draw color if it has changed
        String drawColorStr = drawColorToString(drawColor);
        if (!drawColorStr.equals(lastDrawColor)) {
            code.append("        g2d.setColor(").append(drawColorStr).append(");\n");
            lastDrawColor = drawColorStr;
        }

        // Only set the stroke if it has changed
        if (strokeWidth != lastStrokeWidth) {
            code.append("        g2d.setStroke(new BasicStroke(").append(strokeWidth).append("));\n");
            lastStrokeWidth = strokeWidth;
        }

        // Append shape drawing command
        code.append(shapeDrawCall);

        // Handle filling the shape
        if (fillColor != null) {
            String fillColorStr = drawColorToString(fillColor);
            boolean isFillSameAsStroke = drawColor.getRGB() == fillColor.getRGB();

            if (isFillSameAsStroke) {
                // Draw only filled shape
                if ("Rectangle".equals(type)) {
                    code.append(String.format("        g2d.fillRect(%d, %d, %d, %d);\n",
                            Math.min(start.x, end.x), Math.min(start.y, end.y),
                            Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
                } else if ("Oval".equals(type)) {
                    code.append(String.format("        g2d.fillOval(%d, %d, %d, %d);\n",
                            Math.min(start.x, end.x), Math.min(start.y, end.y),
                            Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
                } else if ("Polygon".equals(type)) {
                    code.append(String.format("        g2d.fillPolygon(new int[] {%s}, new int[] {%s}, %d);\n",
                            xPoints.toString(), yPoints.toString(), polygonPoints.size()));
                }
            } else {
                // Draw filled shape with different colors
                if (!fillColorStr.equals(lastFillColor)) {
                    code.append("        g2d.setColor(").append(fillColorStr).append(");\n");
                    lastFillColor = fillColorStr;
                }

                if ("Rectangle".equals(type)) {
                    code.append(String.format("        g2d.fillRect(%d, %d, %d, %d);\n",
                            Math.min(start.x, end.x), Math.min(start.y, end.y),
                            Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
                } else if ("Oval".equals(type)) {
                    code.append(String.format("        g2d.fillOval(%d, %d, %d, %d);\n",
                            Math.min(start.x, end.x), Math.min(start.y, end.y),
                            Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
                } else if ("Polygon".equals(type)) {
                    code.append(String.format("        g2d.fillPolygon(new int[] {%s}, new int[] {%s}, %d);\n",
                            xPoints.toString(), yPoints.toString(), polygonPoints.size()));
                }
            }
        }
    }
}
