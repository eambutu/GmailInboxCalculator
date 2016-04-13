package sample;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable{

    public TextField codeTextField;
    public Label progressText;
    public Label addressLabel1;
    public Label addressLabel2;
    public Label middleLabel;
    public Label middleLabel2;

    private Service<Void> calculateThread;

    private int curindex = 0;
    private int maxindex;
    private double numcorrect = 0;

    private Pair[] pairs;
    private int[] indicies;
    private String[] topFive;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void onClickLink(){
        new Main().getHostServices().showDocument(GmailAccess.url);
    }

    public void onClickSubmit() throws IOException{
        GmailAccess.authCode(codeTextField.getText());

        calculateThread = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        updateMessage("Working... This may take a few minutes...");
                        GmailAccess.calculate();
                        return null;
                    }
                };
            }
        };

        calculateThread.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                progressText.textProperty().unbind();
                progressText.setText("Done!");
                middleLabel.setText("more important than");
                pairs = GmailAccess.randomsample.clone();
                indicies = GmailAccess.randomindex.clone();
                maxindex = Math.min(20,pairs.length);
                topFive = GmailAccess.getTopFive();
                updateAddressText(curindex);
            }
        });

        progressText.textProperty().bind(calculateThread.messageProperty());

        calculateThread.restart(); //starts the thread
    }

    public void onClickYes() throws IOException{
        System.out.println("clicked yes");
        progressText.setVisible(false);
        if(pairs[indicies[curindex]].importance1>pairs[indicies[curindex]].importance2) numcorrect++;
        if(curindex<maxindex){
            curindex++;
            updateAddressText(curindex);
        }
        else{
            addressLabel1.setText("");
            addressLabel2.setText("");
            middleLabel.setText("Done! Our algorithm was correct "+ (double)Math.round(numcorrect/maxindex*1000)/1000 + " of the time.");
            middleLabel2.setText("Your top five contacts were " + topFive[0] + ", " + topFive[1] + "\n, " + topFive[2] + ", " + topFive[3] + ", " + topFive[4]);
        }
    }

    public void onClickNo() throws IOException{
        System.out.println("clicked no");
        progressText.setVisible(false);
        if(pairs[indicies[curindex]].importance1<pairs[indicies[curindex]].importance2) numcorrect++;
        if(curindex<maxindex){
            curindex++;
            updateAddressText(curindex);
        }
        else{
            addressLabel1.setText("");
            addressLabel2.setText("");
            middleLabel.setText("Done! Our algorithm was correct "+(double)Math.round(numcorrect/maxindex*1000)/1000 + " of the time.");
            middleLabel2.setText("Your top five contacts were " + topFive[0] + ", " + topFive[1] + ", " + topFive[2] + ", " + topFive[3] + ", " + topFive[4]);
        }
    }

    public void updateAddressText(int index){
        addressLabel1.setText("Is "+pairs[indicies[index]].emailaddress1);
        addressLabel2.setText(pairs[indicies[index]].emailaddress2+"?");
        System.out.println("update text: "+index+" "+pairs[indicies[index]].emailaddress1+" "+pairs[indicies[index]].emailaddress2);
    }
}


