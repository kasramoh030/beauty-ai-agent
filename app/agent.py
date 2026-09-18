import os
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY")
)


def ask_agent(message):

    with open("app/knowledge_base.txt", "r", encoding="utf-8") as file:
        knowledge = file.read()

    response = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {
                "role": "system",
                "content": f"""
تو دستیار فروش یک کلینیک زیبایی هستی.

اطلاعات کلینیک:
{knowledge}

قوانین:
- حرفه‌ای و دوستانه پاسخ بده.
- مشتری علاقه‌مند را به رزرو هدایت کن.
- اطلاعات تماس مشتری را درخواست کن.
"""
            },
            {
                "role": "user",
                "content": message
            }
        ]
    )

    return response.choices[0].message.content
