# - 1 get data from know platforms e.g sympla
# - 2 structure and transform data into dataframe
# - 3 create dataframe with columns event, start_date, end_date, type, location
# - 4 generate .csv file with proper dataset

# domains and websites with already filters
# https://www.sympla.com.br/eventos?s=tech, https://www.sympla.com.br/eventos?s=technology, https://www.sympla.com.br/eventos?s=hackathon, https://www.sympla.com.br/eventos?s=tech&dt=2026-08-23%2C2026-09-06, https://www.sympla.com.br/eventos/online?category=collection

# elements
# div class for click to event next page

import asyncio
import httpx
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

client = httpx.AsyncClient()


async def get_data():

    # async with client:
    #     response = await client.get(
    #         "https://www.sympla.com.br/eventos?s=tech"
    #     )
    #     print(response.status_code)
    #     return response.text

    try:
        response = await client.get("https://www.sympla.com.br/eventos?s=tech")
        print(response.status_code)
        soup = BeautifulSoup(response.text, "html.parser")
        print(soup)
        #  page_title = soup.title.string
        #  divs = soup.find_all("div")
        #  for div in divs:
        #     inner_divs = div.find_all("div")
        #     print(inner_divs)
        # h3s = soup.find_all("h3")
        # print(h3s)
        return response.text
    except Exception as error:
        print(f"ERROR: {error}")
    finally:
        await client.aclose()


# html = "<html><body><h1>Hello</h1></body></html>"

# soup = BeautifulSoup(html, "html.parser")

# driver = webdriver.Chrome()

# try:
#     driver.get("https://www.sympla.com.br/eventos?s=tech")

#     print(driver.title)

#     html = driver.page_source
#     print(html)

# finally:
#     driver.quit()


def check_data_by_selenium():

    driver = webdriver.Chrome()
    wait = WebDriverWait(driver, 20)
    driver.get("https://www.sympla.com.br/eventos?s=tech")

    try:
        cookie_button = WebDriverWait(driver, 5).until(
            EC.element_to_be_clickable(
                (
                    By.XPATH,
                    "//button[contains(., 'Aceitar') or contains(., 'Accept')]"
                )
            )
        )
        cookie_button.click()
    except Exception as error:
        print('ERROR: ', error)

    elements = wait.until(
        EC.presence_of_all_elements_located((By.CSS_SELECTOR, "h3.pn67h1f"))
    )

    number_of_events = len(elements)

    for i in range(number_of_events):

        try:
            # elements need to be refreshed
            # code below guarantees DOM refresh
            elements = wait.until(
                EC.presence_of_all_elements_located((By.CSS_SELECTOR, "h3.pn67h1f"))
            )

            element = elements[i]
            print("CLICK: ", element.text)

            # Bring element into the viewport
            link = element.find_element(
                By.XPATH,
                "./ancestor::a[1]"
            )

            driver.execute_script(
                "arguments[0].scrollIntoView({block: 'center'});",
                link
            )

            # Give the browser a moment to finish scrolling/rendering
            wait.until(
                EC.visibility_of(element)
            )

            old_url = driver.current_url
            link.click()

            wait.until(EC.url_changes(old_url))

            print("NEW URL: ", driver.current_url)
            driver.back()
            elements = wait.until(
                EC.presence_of_all_elements_located((By.CSS_SELECTOR, "h3.pn67h1f"))
            )
        except Exception as error:
            print("ERROR: ", error)

    print(len(elements))


if __name__ == "__main__":
    print("Start scraping...")

    check_data_by_selenium()

    # html = asyncio.run(get_data())
    # print(html)
