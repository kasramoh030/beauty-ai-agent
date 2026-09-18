from agent import ask_agent


print("Beauty AI Agent Started")

while True:

    message = input("\nمشتری: ")

    if message == "exit":
        break

    answer = ask_agent(message)

    print("\nAgent:")
    print(answer)
