An example that lets you chat with ChatGPT in your console app.

To use this demo, you must first tell the program your OpenAI key.

* Create an account at https://platform.openai.com/ if you haven't yet.
* Go to https://platform.openai.com/account/api-keys and create a new secret key.
  * This key is shown to you once! Be sure to copy it into your clipboard before exiting the page.
* Define the environment variable "OPENAI_KEY" and set it to the key you copied in the previous step.
  * It's important to keep this key private! Using an environment variable is a way to ensure you won't accidentally put
    it into code that gets uploaded to GitHub.

![Example in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-chatgpt.gif)

This demo is built on the back of GitHub user farmberbb. See also:
* [Their GitHub code gist](https://gist.github.com/farmerbb/72b872a097109cf4613e18a492f8b538)
* [Their Reddit post](https://www.reddit.com/r/Kotlin/comments/11ski80/simple_cli_interface_to_chatgpt_openais_code/)