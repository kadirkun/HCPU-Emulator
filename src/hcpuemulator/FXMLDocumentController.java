/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hcpuemulator;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 *
 * @author mzp7
 */
public class FXMLDocumentController implements Initializable {
    
    @FXML
    private TableView<TableCell> table;
    @FXML
    private TextArea textarea;
    @FXML
    private TextField addresstext;
    @FXML
    private TextField valuetext;
    
    
    private Map<String, String> opcodes;
    
    class TableCell{
        public int address;
        public int value;
    }
    
    class InstructionMem{
        ArrayList<Instruction> instructions = new ArrayList<>();
        int pc = 0;
        
        class Instruction{
            public String opcode;
            public String addressA;
            public String addressB;
            ByteBuffer inst;

            public Instruction(String source) {
                String[] parts = source.trim().split(" ");
                opcode = parts[0];
                addressA = parts[1];
                addressB = parts[2];
            }
            
            public int getAddressA(){
                return Integer.valueOf(this.addressA);
            }
            
            public int getAddressB(){
                return Integer.valueOf(this.addressB);
            }
        }

        public InstructionMem() {
        }
        
        public void addInstruction(String source){
            if(source.matches("^.*\\w+\\s\\d+\\s\\d+$")){
                instructions.add(new Instruction(source));
            }else{
                throw new IllegalArgumentException();
            }
        }
        
        public void runAll(){
            for(int i = 0; i < instructions.size(); i++){
                run();
            }
        }
        
        public void run(){
            if(!(instructions.size() > pc)){
                return;
            }
            Instruction inst = instructions.get(pc);
            switch(inst.opcode){
                case "ADD":
                    setValue(inst.getAddressA(), getValue(inst.getAddressA()) + getValue(inst.getAddressB()));
                    break;
                case "ADDi":
                    setValue(inst.getAddressA(), getValue(inst.getAddressA()) + inst.getAddressB());
                    break;
                case "NAND":
                    setValue(inst.getAddressA(), ~(getValue(inst.getAddressA()) & getValue(inst.getAddressB())));
                    break;
                case "NANDi":
                    setValue(inst.getAddressA(), ~(getValue(inst.getAddressA()) & inst.getAddressB()));
                    break;
                case "SRL":
                    setValue(inst.getAddressA(), getValue(inst.getAddressA()) >> (getValue(inst.getAddressB() % 32)));
                    break;
                case "SRLi":
                    setValue(inst.getAddressA(), getValue(inst.getAddressA()) << (getValue(inst.getAddressB() % 32)));
                    break;
                case "LT":
                    if(getValue(inst.getAddressA()) < getValue(inst.getAddressB())){
                        setValue(inst.getAddressA(), 1);
                    } else {
                        setValue(inst.getAddressA(), 0);
                    }
                    break;
                case "LTi":
                    if(getValue(inst.getAddressA()) < inst.getAddressB()){
                        setValue(inst.getAddressA(), 1);
                    } else {
                        setValue(inst.getAddressA(), 0);
                    }
                    break;
                case "CP":
                    setValue(inst.getAddressA(), getValue(inst.getAddressB()));
                    break;
                case "CPi":
                    setValue(inst.getAddressA(), inst.getAddressB());
                    break;
                case "CPI":
                    setValue(inst.getAddressA(), getValue(getValue(inst.getAddressB())));
                    break;
                case "CPIi":
                    setValue(getValue(inst.getAddressA()), getValue(inst.getAddressB()));
                    break;
                case "BZJ":
                    if(getValue(inst.getAddressB()) == 0){
                        pc = getValue(inst.getAddressA());
                    }
                    break;
                case "BZJi":
                    pc = getValue(inst.getAddressA()) + inst.getAddressB();
                    pc--;
                    break;
                case "MUL":
                    setValue(inst.getAddressA(), getValue(inst.getAddressA()) * getValue(inst.getAddressB()));
                    break;
                case "MULi":
                    setValue(inst.getAddressA(), getValue(inst.getAddressA()) * inst.getAddressB());
                    break;
                default:
                    System.err.println("Unknown OP : " + inst.opcode);
            }
            pc++;
            table.refresh();
        }
    }
    
    private HashMap<Integer, TableCell> memory = new HashMap<>(256);
    
    @FXML
    public void openhelppage(){
        try {
            Parent root = FXMLLoader.load(getClass().getResource("InfoPage.fxml"));
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e){
            System.err.println("Error at openhelppage | " + e);
        }
    }
    
    @FXML
    public void runscript(ActionEvent event){
        String script = textarea.getText();
        
        Scanner input = new Scanner(script);
        
        InstructionMem insts = new InstructionMem();
        
        while(input.hasNext()){
            insts.addInstruction(input.nextLine());
        }
        
        insts.runAll();
    }
    
    @FXML
    public void clearmemory(){
        table.getItems().clear();
        memory.clear();;
    }
    
    @FXML
    public void insertToMemory(){
        if(addresstext.getText().length() > 0 && valuetext.getText().length() > 0){
            setValue(Integer.valueOf(addresstext.getText()), Integer.valueOf(valuetext.getText()));
            table.refresh();
        }
    }
    
    public TableCell allocateMemory(int bbuff){
        if(bbuff >= 0 && bbuff < Math.pow(2, 32)){
            TableCell newCell = new TableCell();
            newCell.address = bbuff;
            memory.put(newCell.address, newCell);
            table.getItems().add(newCell);
            return newCell;
        }else{
            throw new OutOfMemoryError();
        }
    }
    
    public int getValue(int bbuff){
        if(memory.containsKey(bbuff)){
            TableCell cell = memory.get(bbuff);
            return cell.value;
        }else{
            return allocateMemory(bbuff).value;
        }
    }
    
    public void setValue(int bbuff, int value){
        if(memory.containsKey(bbuff)){
            TableCell cell = memory.get(bbuff);
            cell.value = value;
        }else{
            allocateMemory(bbuff).value = value;
        }
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addresstext.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, 
                String newValue) {
                if (!newValue.matches("\\d*")) {
                    addresstext.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
        
        valuetext.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, 
                String newValue) {
                if (!newValue.matches("\\d*")) {
                    valuetext.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
        
        TableColumn<TableCell, Integer> addresscolumn = new TableColumn<>("Addresses");
        TableColumn<TableCell, Integer> valuecolumn   = new TableColumn<>("Values");
        
        addresscolumn.setCellValueFactory((param) -> {
            return new ReadOnlyIntegerWrapper(param.getValue().address).asObject(); //To change body of generated lambdas, choose Tools | Templates.
        });
        
        valuecolumn.setCellValueFactory((param) -> {
            return new ReadOnlyIntegerWrapper(param.getValue().value).asObject(); //To change body of generated lambdas, choose Tools | Templates.
        });
        
        table.getColumns().addAll(addresscolumn, valuecolumn);
    }    
    
}