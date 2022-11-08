package com.afunproject.dawncraft.client.screens;

import java.util.List;
import java.util.Random;

import com.afunproject.dawncraft.client.EntityRenderDispatcherExtension;
import com.afunproject.dawncraft.network.AFPPacketHandler;
import com.afunproject.dawncraft.network.TriggerQuestCompleteMessage;
import com.afunproject.dawncraft.quest.QuestType;
import com.feywild.quest_giver.screen.button.QuestButton;
import com.feywild.quest_giver.screen.button.QuestButtonSmall;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Mob;

public class QuestScreen extends Screen {

	private static final int TEXT_WIDTH = 56;

	protected final Mob entity;
	protected final QuestType questType;
	protected List<Page> pages = Lists.newArrayList();
	protected Random random = new Random();
	protected final Style style;

	protected int screenIndex = 0;

	public QuestScreen(Mob entity, MutableComponent text, QuestType questType) {
		super(entity.getName());
		this.entity = entity;
		this.questType = questType;
		style = text.getStyle();
		generateLines(text);
	}

	private void generateLines(MutableComponent text) {
		if (text != null) {
			String str = text.getString();
			int position = 0;
			List<String> lines = Lists.newArrayList();
			if (str.length() == 0) lines.add(new TranslatableComponent("text.afptweaks.quest.no_text").getString());
			while (position < str.length()) {
				int size = Math.min(TEXT_WIDTH, str.length()-position);
				int newPos = position + size;
				if (str.substring(position, newPos).contains("�")) {
					int i = str.substring(position, newPos).indexOf("�");
					lines.add(str.substring(position, position + i));
					pages.add(new TextPage(this, lines));
					lines = Lists.newArrayList();
					position = position + i + 1;
					continue;
				}
				if (size < TEXT_WIDTH) {
					lines.add(str.substring(position));
					break;
				}
				for (int i = 0; i <= size; i++) {
					if (i == size) {
						lines.add(str.substring(position, newPos+1));
						position = newPos;
						if (lines.size() >= 10) {
							pages.add(new TextPage(this, lines));
							lines = Lists.newArrayList();
						}
						break;
					}
					else if (str.charAt(newPos-i) == ' ') {
						lines.add(str.substring(position, newPos-i+1));
						position = newPos-i+1;
						if (lines.size() >= 10) {
							pages.add(new TextPage(this, lines));
							lines = Lists.newArrayList();
						}
						break;
					}
				}

			}
			pages.add(new TextPage(this, lines));
		} else {
			pages.add(new TextPage(this, Lists.newArrayList(new TranslatableComponent("text.afptweaks.quest.no_text", "null").getString())));
		}
	}

	@Override
	protected void init() {
		if (pages.size() == 1 && questType != QuestType.AUTO_CLOSE) {
			addAcceptButtons();
		} else {
			addRenderableWidget(new QuestButtonSmall(380, 120, true, entity.blockPosition(), new TextComponent(">>"), button -> {
				if (screenIndex == pages.size()-1) {
					completeQuest(true);
					onClose();
					return;
				}
				screenIndex++;
				if (!hasNextButton() && questType != QuestType.AUTO_CLOSE) {
					removeWidget(button);
					addAcceptButtons();
				}
			}));
		}
	}

	private void completeQuest(boolean accepted) {
		AFPPacketHandler.NETWORK_INSTANCE.sendToServer(new TriggerQuestCompleteMessage(entity, accepted));
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		int entityX = 37;
		int entityY = 240;
		int size = 65;
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		((EntityRenderDispatcherExtension)dispatcher).setRenderNameplate(false);
		InventoryScreen.renderEntityInInventory(entityX, entityY, size, entityX - mouseX, entityY + (entity.getEyeHeight()) - mouseY, entity);
		((EntityRenderDispatcherExtension)dispatcher).setRenderNameplate(true);
		drawString(poseStack, minecraft.font, title, (width / 2) - (minecraft.font.width(title)), 125, 0xFFFFFF);
		if (pages.size() > 0) {
			pages.get(screenIndex).render(poseStack, mouseX, mouseY, partialTicks);
		}
		super.render(poseStack, mouseX, mouseY, partialTicks);
	}

	private boolean hasNextButton() {
		return screenIndex < pages.size() -1;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private void addAcceptButtons() {
		if (questType == QuestType.ACCEPT_QUEST) {
			addRenderableWidget(new QuestButton(380, 190, true, entity.blockPosition(), new TextComponent(getRandomAcceptMessage()), button1 -> {
				completeQuest(true);
				onClose();
				return;
			}));

			addRenderableWidget(new QuestButton(380, 218, false, entity.blockPosition(), new TextComponent(getRandomDeclineMessage()), button1 -> {
				completeQuest(false);
				onClose();
				return;
			}));
		}
		else if (questType == QuestType.ACKNOWLEDGE) {
			String[] messages = getRandomAcknowledgmentMessages();
			addRenderableWidget(new QuestButton(380, 190, true, entity.blockPosition(), new TextComponent(messages[0]), button1 -> {
				completeQuest(true);
				onClose();
				return;
			}));

			addRenderableWidget(new QuestButton(380, 218, false, entity.blockPosition(), new TextComponent(messages[1]), button1 -> {
				completeQuest(true);
				onClose();
				return;
			}));
		}
		else if (questType == QuestType.DENY) {
			String[] messages = getRandomDenyMessages();
			addRenderableWidget(new QuestButton(380, 190, true, entity.blockPosition(), new TextComponent(messages[0]), button1 -> {
				completeQuest(false);
				onClose();
				return;
			}));

			addRenderableWidget(new QuestButton(380, 218, false, entity.blockPosition(), new TextComponent(messages[1]), button1 -> {
				completeQuest(false);
				onClose();
				return;
			}));
		}
	}

	private String[] getRandomAcknowledgmentMessages() {
		String[] messages = new String[2];
		messages[0] = getRandomAcknowledgmentMessage();
		while (messages[1] == null) {
			String message = getRandomAcknowledgmentMessage();
			if (messages[0] != message) messages[1] = message;
		}
		return messages;
	}

	private String[] getRandomDenyMessages() {
		String[] messages = new String[2];
		messages[0] = getRandomDenyMessage();
		while (messages[1] == null) {
			String message = getRandomDenyMessage();
			if (messages[0] != message) messages[1] = message;
		}
		return messages;
	}

	private String getRandomAcknowledgmentMessage() {
		return switch (random.nextInt(5)) {
		case 1 -> "Got it";
		case 2 -> "Sure";
		case 3 -> "Okay!";
		case 4 -> "Alright!";
		default -> "Thanks!";
		};
	}

	private String getRandomDenyMessage() {
		return switch (random.nextInt(4)) {
		case 1 -> "Not yet...";
		case 2 -> "Working on it";
		case 3 -> "Sorry, no";
		default -> "No";
		};
	}

	private String getRandomAcceptMessage() {
		return switch (random.nextInt(5)) {
		case 1 -> "I'll do it!";
		case 2 -> "Sure";
		case 3 -> "Okay!";
		case 4 -> "Alright!";
		default -> "I accept!";
		};
	}

	private String getRandomDeclineMessage() {
		return switch (random.nextInt(5)) {
		case 1 -> "Maybe later.";
		case 2 -> "I'm Busy";
		case 3 -> "No, Sorry.";
		case 4 -> "Not right now";
		default -> "I decline!";
		};
	}

}
