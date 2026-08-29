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
import time

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

    driver.get(
        "https://www.sympla.com.br/eventos?s=tech&dt=2026-08-23%2C2026-09-06"
    )

    link_list = []

    try:
        while True:

            # Get cards
            elements = wait.until(
                EC.presence_of_all_elements_located(
                    (By.CSS_SELECTOR, "a.sympla-card")
                )
            )

            # Remember current page
            old_href = elements[0].get_attribute("href")

            # Collect links
            for element in elements:

                href = element.get_attribute("href")

                if href and href not in link_list:
                    link_list.append(href)

            print(f"TOTAL LINKS: {len(link_list)}")

            # Find next
            try:

                button_next_page = WebDriverWait(driver, 5).until(
                    EC.element_to_be_clickable(
                        (
                            By.XPATH,
                            "//button[.//div[contains(@class, '_2pl8g9g')]]"
                        )
                    )
                )

            except Exception as error:

                print("No more pages.")
                break

            button_next_page = wait.until(
    EC.presence_of_element_located(
        (
            By.XPATH,
            "//button[.//div[contains(@class, '_2pl8g9g')]]"
        )
    )
)

            driver.execute_script("""
                arguments[0].scrollIntoView({
                    behavior: 'instant',
                    block: 'center',
                    inline: 'center'
                });
            """, button_next_page)

            # Check position AFTER scrolling
            rect = driver.execute_script("""
    const r = arguments[0].getBoundingClientRect();

    return {
        top: r.top,
        bottom: r.bottom,
        left: r.left,
        right: r.right,
        viewportHeight: window.innerHeight
    };
""", button_next_page)

            print("BUTTON AFTER SCROLL:", rect)

            button_next_page.click()

            # Wait for card content to change
            wait.until(
                lambda driver:
                    driver.find_element(
                        By.CSS_SELECTOR,
                        "a.sympla-card"
                    ).get_attribute("href") != old_href
            )
    except Exception as error:

        print("SCRAPER ERROR:", error)

    finally:

        driver.quit()

    return link_list

def check_button_click():

    driver = webdriver.Chrome()
    wait = WebDriverWait(driver, 20)

    driver.get(
        "https://www.sympla.com.br/eventos?s=tech&dt=2026-08-23%2C2026-09-06"
    )

    count = 1

    elements = wait.until(
                EC.presence_of_all_elements_located(
                    (By.CSS_SELECTOR, "a.sympla-card")
                )
            )
    
    time.sleep(5)

    button_number = wait.until(
                    EC.element_to_be_clickable(
                        (
                            By.XPATH,
                            f"//li//a[text()='{count+1}']"
                        )
                    )
                )

    button_number.click()  

    # button_next_page = wait.until(
    #                 EC.element_to_be_clickable(
    #                     (
    #                         By.XPATH,
    #                         "//button[.//div[contains(@class, '_2pl8g9g')]]"
    #                     )
    #                 )
    #             )
    
    # button_next_page.click()


if __name__ == "__main__":
    print("Start scraping...")

    check_button_click()

    # link_list = check_data_by_selenium()

    # html = asyncio.run(get_data())
    # print(html)
