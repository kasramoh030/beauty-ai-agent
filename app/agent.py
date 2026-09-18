import os
from dotenv import load_dotenv
from google import genai
from google.genai import types

load_dotenv()

client = genai.Client(
    api_key=os.getenv("GEMINI_API_KEY")
)


def ask_agent(message):
    with open("app/knowledge_base.txt", "r", encoding="utf-8") as file:
        knowledge = file.read()

    system_instruction = f"""
تو دستیار فروش یک کلینیک زیبایی هستی.

اطلاعات کلینیک:
{knowledge}

قوانین:
- حرفه‌ای و دوستانه پاسخ بده.
- پاسخ‌ها کوتاه و مفید باشند.
- اگر مشتری درباره خدمات سؤال کرد، بر اساس اطلاعات کلینیک پاسخ بده.
- اگر مشتری علاقه‌مند بود، او را به رزرو هدایت کن.
- در صورت نیاز اطلاعات تماس مشتری را درخواست کن.
- اطلاعاتی که در knowledge base وجود ندارد را از خودت نساز.
"""

    response = client.models.generate_content(
        model="gemini-3.8-flash",
        contents=message,
        config=types.GenerateContentConfig(
            system_instruction=system_instruction
        )
    )

    return response.text
