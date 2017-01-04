package com.github.mohdaminyuddin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendLocation;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.github.mohdaminyuddin.domain.Data;
import com.github.mohdaminyuddin.domain.ReportResponse;

@Component
public class Bot extends TelegramLongPollingBot  {
	
	private static final Logger logger = LoggerFactory.getLogger(Bot.class);
	
	@Autowired
	RestTemplate restTemplate;
	
	@Value("${telegram.token}")
	private String token;

	@Value("${telegram.username}")
	private String username;

	@Override
	public String getBotUsername() {
		return username;
	}
	
	@Override
	public String getBotToken() {
		return token;
	}

	@Override
	public void onUpdateReceived(Update update) {
		
		if(update.hasCallbackQuery()){
			String data = update.getCallbackQuery().getData();
			long chatId = update.getCallbackQuery().getMessage().getChatId();
			
			if(data.startsWith("[A]")){		
				SendMessage sendMessage = new SendMessage();
				
				AnswerCallbackQuery a = new AnswerCallbackQuery();
				a.setCallbackQueryId(update.getCallbackQuery().getId());
				try {
					answerCallbackQuery(a);
				} catch (TelegramApiException e) {
					logger.info("Error sending answer");
				}
				
				try {
					ReportResponse r = getData(1, data.substring(3));
					this.responseData(r, sendMessage, chatId);
				} catch (URISyntaxException e) {
					logger.info("Error to get list for whole state");
				}
			}
			
			if(data.startsWith("[N]")){
				List<String> daerah = this.getDaerah(update.getCallbackQuery().getData().substring(3));
				
				AnswerCallbackQuery a = new AnswerCallbackQuery();
				a.setCallbackQueryId(update.getCallbackQuery().getId());
				try {
					answerCallbackQuery(a);
				} catch (TelegramApiException e) {
					logger.info("Error sending answer");
				}
				
				SendMessage sendMessage = new SendMessage();
				
				sendMessage.setChatId(chatId);
				sendMessage.setText("Daerah : ");
							
				this.returnInlineButton(daerah, "[D]", sendMessage);
			}
			
			if(data.startsWith("[D]")){
				SendMessage sendMessage = new SendMessage();
				
				AnswerCallbackQuery a = new AnswerCallbackQuery();
				a.setCallbackQueryId(update.getCallbackQuery().getId());
				try {
					answerCallbackQuery(a);
				} catch (TelegramApiException e) {
					logger.info("Error sending answer");
				}
				
				try {
					ReportResponse r = getData(2, data.substring(3));
					this.responseData(r, sendMessage, chatId);
				} catch (URISyntaxException e) {
					logger.info("Error to get list for specified daerah");
				}
				
			}
			
		}
		
		if (update.hasMessage() && !update.hasCallbackQuery()) {

			Message message = update.getMessage();
						
			SendMessage sendMessage = new SendMessage();
			Long chatId = message.getChatId();
			Integer messageId = message.getMessageId();
			
			sendMessage.setChatId(chatId);
			
			String text = message.getText();
			
			ReplyKeyboardMarkup replyKeyboardMarkup = sendHelpMessage(chatId, messageId);
			
			if (replyKeyboardMarkup != null) {
	            sendMessage.setReplyMarkup(replyKeyboardMarkup);
	        }
			
			//Dirty hack to process feedback message.
			if(message.getReplyToMessage()!=null){
				if(message.getReplyToMessage().getText().equals("Sila hantar cadangan dan maklum balas anda.")){
					sendMessage.setParseMode("Markdown");
					this.forwardFeedback(sendMessage, message);
					sendMessage.setChatId(chatId);
					sendMessage.setText("*Terima kasih* di atas cadangan/maklum balas anda. Kami amat menghargai sokongan anda");
					sendMessageToUser(sendMessage);
				} else{
					sendMessage.setChatId(chatId);
					sendMessage.setParseMode("Markdown");
					sendMessage.setText("*Maaf* Arahan anda tidak sah.");
					sendMessageToUser(sendMessage);
				}
			} else{
				
				if(text.equals("💼 About")){
					
					this.sendLog(message, text);
					
					sendMessage.setChatId(chatId);
					sendMessage.setParseMode("Markdown");
					sendMessage.setText(getBotInfo());
					sendMessageToUser(sendMessage);
					
				} else if(text.equals("✉️ Cadangan")){
					
					this.sendLog(message, text);
					
					sendMessage.setReplyMarkup(new ForceReplyKeyboard());
					sendMessage.setChatId(chatId);
					sendMessage.setParseMode("Markdown");
					sendMessage.setText(requestSuggestion());
					sendMessageToUser(sendMessage);
				} else if(text.equals("🔍 Carian")){
					
					List<String> negeri = new ArrayList<>();
					try {
						ReportResponse r = getData(0, null);
						this.sendLog(message, text);
						for (Data ret : r.getData()) {
							negeri.add(ret.getNegeri());
						}
					} catch (Exception e) {
						logger.info("Failed to get state list");
					}
					
					/*
					 * Remove duplicate ArrayList
					 */
					this.removeDuplicates(negeri);
					sendMessage.setText("Negeri : ");
					this.returnInlineButton(negeri, "[N]", sendMessage);
				}else if(text.equals("🌐 Senarai penuh")){
					try {
						ReportResponse r = getData(0, null);
						
						this.sendLog(message, text);
											
						this.responseData(r, sendMessage, chatId);
						
					} catch (URISyntaxException e) {
						logger.error("Failed to get response from API");
					}
					
				}else{				
			        sendMessage.setReplyToMessageId(messageId);

			        if (replyKeyboardMarkup != null) {
			            sendMessage.setReplyMarkup(replyKeyboardMarkup);
			        }

			        sendMessage.setText("Pilihan : ");
					sendMessageToUser(sendMessage);
				}
				
			}
		}
	}
	
	private void forwardFeedback(SendMessage sendMessage, Message message) {
		sendMessage.setChatId(000000L); //whatever chatId you what to forward the feedback to. (or you can proceed to store or send somewhere else (e.g. email)
		sendMessage.setParseMode("Markdown");
		sendMessage.setText("*[Cadangan]* : @" + message.getFrom().getUserName() + "  " + message.getFrom().getFirstName() + "  "  + message.getFrom().getLastName() + "\n" + message.getText());
		sendMessageToUser(sendMessage);
	}

	private List<String> getDaerah(String negeri) {
		List<String> daerah = new ArrayList<String>();
		try {
			ReportResponse r = getData(1, negeri);
			for (Data ret : r.getData()) {
				daerah.add(ret.getDaerah());
			}
			daerah.add(negeri);
		} catch (Exception e) {
			logger.info("Failed to get city list");
		}
		this.removeDuplicates(daerah);
		return daerah;
	}

	private void sendMessageToUser(SendMessage sendMessage){
		try {
			sendMessage(sendMessage);
		} catch (TelegramApiException e) {
			logger.error("Failed to send message : {}", e.getMessage());
		}
	}
	
	private static String getBotInfo(){
		return "*MyBanjir Telegram Bot*\n\n_Dibangunkan oleh_ :\n*Bot* : @mohdaminyuddin\n*API* : @zulhilmizainudin\n\n"+
				"[Lihat dokumentasi penuh API](https://zulhfreelancer.github.io/banjir-api/)\n";
	}
	
	private static String requestSuggestion(){
		return "*Sila* hantar cadangan dan maklum balas anda.";
	}
	
	private static ReplyKeyboardMarkup sendHelpMessage(Long chatId, Integer messageId) {

		ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);
        
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add("🔍 Carian");
        keyboardFirstRow.add("🌐 Senarai penuh");
        
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add("✉️ Cadangan");
        keyboardSecondRow.add("💼 About");
        
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;

    }
	
	private ReportResponse getData(int type, String param) throws URISyntaxException{
		
		String s = "https://banjir-api.herokuapp.com/api/v1/reports.json";
		if(type == 1){
			s = "https://banjir-api.herokuapp.com/api/v1/reports.json?negeri=" + param;
		} else if(type == 2){
			s = "https://banjir-api.herokuapp.com/api/v1/reports.json?daerah=" + param;
		} 
		
		s=s.replace(" ", "%20");
		URI url = new URI(s);
		ReportResponse r = restTemplate.getForObject(url, ReportResponse.class);
		return r;
		
	}
	
	private List<String> removeDuplicates(List<String> list) {
		Set<String> setItems = new LinkedHashSet<String>(list);
		list.clear();
		list.addAll(setItems);
		return list;
	}
	
	private void returnInlineButton(List<String> param, String prefix, SendMessage sendMessage) {
		InlineKeyboardMarkup re = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> kr = new ArrayList<>();
		
		String icon = "";
		
		if(prefix.equals("[D]")){
			List<InlineKeyboardButton> kbAll = new ArrayList<>();
			kbAll.add(new InlineKeyboardButton().setText("🌍 Seluruh negeri").setCallbackData("[A]" + param.get(param.size()-1)));
			kr.add(kbAll);
			param.remove(param.size()-1);
			icon = "🔆 ";
		} else{
			icon = "🇲🇾 ";
		}
		
		
		List<InlineKeyboardButton> kb = new ArrayList<>();
		InlineKeyboardButton x = null;

		for (int i = 1; i <= param.size(); i++) {
			if (i % 2 != 0) {
				kb = new ArrayList<>();
			}

			x = new InlineKeyboardButton().setText(icon + param.get(i - 1)).setCallbackData(prefix + param.get(i - 1));
			kb.add(x);

			if (i % 2 == 0 || i == param.size()) {
				kr.add(kb);
			}
		}

		re.setKeyboard(kr);
		sendMessage.setReplyMarkup(re);
		sendMessageToUser(sendMessage);
	}
	
	
	private void sendLog(Message message, String action){
		//Code for whatever related to statistic here
	}
	
	private void responseData(ReportResponse r, SendMessage sendMessage, long chatId) {
		String markupResponse="";
		
		for (Data ret : r.getData()) {
			
			String waterLevel = (ret.getParas_air() == null) ? "Tiada maklumat" : ret.getParas_air() + " meter";
						
			String streetName = (ret.getNama_jalan() != null) ? ret.getNama_jalan() : ret.getNama_laluan();
			
			markupResponse = "*" + ret.getDaerah() + ", " + ret.getNegeri() + "*\n" + "_" + streetName + "_\n"
					+ "```text\n" + "\n" + "Aras air : " + waterLevel + "\n" + "Status : " + ret.getStatus()
					+ "\n" + "```\n" + "[Lihat di Google Maps](" + ret.getGoogle_maps_url() + ")\n";
			
			sendMessage.setParseMode("Markdown");
			sendMessage.setText(markupResponse);
			sendMessage.setChatId(chatId);
			sendMessageToUser(sendMessage);
			SendLocation sL = new SendLocation();
			sL.setChatId(chatId);
			sL.setLatitude(ret.getLatitude());
			sL.setLongitude(ret.getLongitude());
			try {
				sendLocation(sL);
			} catch (TelegramApiException e) {
				logger.error("Failed to send location");
			}

		}
	}

}
