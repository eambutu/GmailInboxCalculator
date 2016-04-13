package sample;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class GmailAccess {
    // Check https://developers.google.com/gmail/api/auth/scopes for all available scopes
    private static final String SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private static final String APP_NAME = "Gmail Inbox Importance";
    // Email address of the user, or "me" can be used to represent the currently authorized user.
    private static final String USER = "me";
    // Path to the client_secret.json file downloaded from the Developer Console
    private static final String CLIENT_SECRET_PATH = "./client_secret_1031338574393-gkdp69om7q0cpgvcijjtqj1r87r5gdqe.apps.googleusercontent.com.json";

    private static GoogleClientSecrets clientSecrets;
    private static HttpTransport httpTransport;
    private static JsonFactory jsonFactory;
    private static GoogleAuthorizationCodeFlow flow;

    public static String url;
    public static Gmail service;

    private static HashMap<String,Integer> freq = new HashMap<String,Integer>();
    private static HashMap<Integer,String> contacts = new HashMap<Integer, String>();
    private static HashMap<String,Integer> index = new HashMap<String,Integer>();
    private static double[][] relations;
    private static String[] emailaddressses;
    private static double[] importance;

    public static int[] randomindex;
    public static Pair[] randomsample;

    public static String generateURL() throws IOException{
        System.out.println(System.getProperty("user.dir"));
        httpTransport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();

        clientSecrets = GoogleClientSecrets.load(jsonFactory,  new FileReader(CLIENT_SECRET_PATH));

        // Allow user to authorize via url.
        flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, Arrays.asList(SCOPE))
                .setAccessType("online")
                .setApprovalPrompt("auto").build();

        url = flow.newAuthorizationUrl().setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
                .build();
        return url;
    }

    public static void authCode(String code) throws IOException{
        // Generate Credential using retrieved code.
        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).execute();
        GoogleCredential credential = new GoogleCredential()
                .setFromTokenResponse(response);

        // Create a new authorized Gmail API client
        service = new Gmail.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(APP_NAME).build();
    }

    public static void calculate() throws IOException{

        int curindex = 0;

        //Retrieve messages
        List<String> labelIDs = new LinkedList<String>();
        labelIDs.add("SENT");
        ListMessagesResponse messagesResponse = service.users().messages().list(USER).setLabelIds(labelIDs).setQ("-is:chat").execute();
        List<Message> messages = messagesResponse.getMessages();

        //work with messages
        for(Message temp : messages){
            Message message = service.users().messages().get(USER,temp.getId()).execute();
            if(message.getPayload()==null){
                System.out.println("None");
            }
            else { //read the receiver
                List<MessagePartHeader> headers = message.getPayload().getHeaders();
                for (MessagePartHeader header : headers) {
                    if (header.getName().equals("To")) {
                        String recipient = getEmailAddress(header.getValue());
                        System.out.println(temp.getId() + " " + recipient);
                        if(!contacts.containsValue(recipient)){
                            contacts.put(recipient.hashCode(),recipient);
                            index.put(recipient,curindex);
                            freq.put(recipient,1);
                            curindex++;
                        }
                        else{
                            Integer count = freq.get(recipient);
                            freq.put(recipient, (count==null) ? 1 : count+1);
                        }
                        break;
                    }
                }
            }
        }

        //gets query string and creates matrix
        String test="";
        relations = new double[curindex][curindex];
        emailaddressses = new String[curindex];
        for(String temp:contacts.values()){
            test += "from:"+temp+" OR ";
            emailaddressses[index.get(temp)] = temp;
            relations[index.get(temp)][index.get(temp)] += freq.get(temp);
            System.out.println(temp+" "+index.get(temp)+" "+freq.get(temp));
        }
        test = test.substring(0,test.length()-3);
        //System.out.println(test);

        //Retrieve messages
        labelIDs = new LinkedList<String>();
        labelIDs.add("INBOX");
        messagesResponse = service.users().messages().list(USER).setLabelIds(labelIDs).setQ(test+" -is:chat ").execute();
        messages = messagesResponse.getMessages();

        //work with messages
        for(Message temp : messages){
            Message message = service.users().messages().get(USER,temp.getId()).execute();
            if(message.getPayload()==null){
                System.out.println("None");
            }
            else { //read the sender
                List<MessagePartHeader> headers = message.getPayload().getHeaders();
                for (MessagePartHeader header : headers) {
                    if (header.getName().equals("From")) {
                        String sender = getEmailAddress(header.getValue());
                        relations[index.get(sender)][index.get(sender)]++;
                        System.out.println(temp.getId() + " " + sender);
                        break;
                    }
                }
            }
        }

        //calculates importance vector
        importance = MatrixCalculator.findImportance(relations);

        for(int k=0;k<emailaddressses.length;k++){
            System.out.println(emailaddressses[k]+" "+importance[k]);
        }

        //get random sampling to ask
        randomsample = new Pair[emailaddressses.length*(emailaddressses.length+1)/2];
        randomindex = new int[emailaddressses.length*(emailaddressses.length+1)/2];
        curindex = 0;
        for(int i=0;i<emailaddressses.length;i++){
            for(int j=i+1;j<emailaddressses.length;j++){
                randomsample[curindex] = new Pair(emailaddressses[i],emailaddressses[j],importance[i],importance[j]);
                randomindex[curindex] = curindex;
                curindex++;
            }
        }
        randomize();
    }

    public static String getEmailAddress(String header){
        String[] temp = header.split(" ");

        String address = temp[temp.length-1];
        if(address.charAt(0)=='<'){
            return address.substring(1,address.length()-1);
        }
        return address;
    }

    public static void randomize(){
        for(int i=randomindex.length-1;i>0;i--){
            int tempindex = (int)(Math.random()*i);
            int temp = randomindex[i];
            randomindex[i] = randomindex[tempindex];
            randomindex[tempindex] = temp;
        }
    }

    public static String[] getTopFive(){
        if (emailaddressses.length <= 5) {
            return emailaddressses;
        }
        int[] indexOfTopFive = {-1, -1, -1, -1, -1};
        for (int index = 0; index < emailaddressses.length; ++index) {
            for (int index2 = 0; index2 < 5; ++index2) {
                if (indexOfTopFive[index2] == -1 || importance[index] > importance[indexOfTopFive[index2]]) {
                    for (int index3 = 4; index3 > index2; --index3) {
                        indexOfTopFive[index3] = indexOfTopFive[index3 - 1];
                    }
                    indexOfTopFive[index2] = index;
                    break;
                }
            }
        }
        return new String[] {emailaddressses[indexOfTopFive[0]],emailaddressses[indexOfTopFive[1]],emailaddressses[indexOfTopFive[2]],emailaddressses[indexOfTopFive[3]],emailaddressses[indexOfTopFive[4]]};
    }
}
