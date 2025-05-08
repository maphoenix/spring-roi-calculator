import requests
import itertools
import time
from pprint import pprint

# Base URL for the API
base_url = "http://localhost:8080/api/roi/calculate"
headers = {"Content-Type": "application/json"}

# Define parameter options with sensible defaults
param_options = {
    "solarPanelDirection": ["south", "east", "west", "north"],
    "haveOrWillGetEv": [True, False],
    "homeOccupancyDuringWorkHours": [True, False],
    "needFinance": [True, False],
    "batterySize": [0, 5.0, 10.0, 13.5, 17.5],  # 0 means no battery
    "usage": [2000, 3000, 4000, 5000, 6000],  # kWh per year
    "solarSize": [2.0, 3.0, 4.0, 5.0, 6.0],  # kW
    "includePdfBreakdown": [True]  # Keep this always true for testing
}

# Function to generate combinations while limiting total to desired range
def generate_test_cases(min_tests=100, max_tests=200):
    # Generate all possible combinations
    keys = list(param_options.keys())
    all_values = [param_options[key] for key in keys]
    all_combinations = list(itertools.product(*all_values))
    
    total_combinations = len(all_combinations)
    print(f"Total possible combinations: {total_combinations}")
    
    # If total combinations are within range, use all
    if min_tests <= total_combinations <= max_tests:
        return [dict(zip(keys, combo)) for combo in all_combinations]
    
    # If too many combinations, sample a subset
    if total_combinations > max_tests:
        # Create a reasonable subset by skipping some options
        step = total_combinations // max_tests + 1
        sampled_combinations = all_combinations[::step][:max_tests]
        return [dict(zip(keys, combo)) for combo in sampled_combinations]
    
    # If too few combinations, create additional variations
    # For this scenario, let's vary battery sizes and usages more granularly
    refined_options = param_options.copy()
    refined_options["batterySize"] = [0, 2.5, 5.0, 7.5, 10.0, 12.5, 13.5, 15.0, 17.5, 20.0]
    refined_options["usage"] = [1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500]
    
    keys = list(refined_options.keys())
    all_values = [refined_options[key] for key in keys]
    all_combinations = list(itertools.product(*all_values))
    
    # Sample to get within max_tests
    step = max(1, len(all_combinations) // max_tests)
    sampled_combinations = all_combinations[::step][:max_tests]
    return [dict(zip(keys, combo)) for combo in sampled_combinations]

# Generate the test cases
test_cases = generate_test_cases()
print(f"Generated {len(test_cases)} test cases")

# Function to make API requests
def test_api(test_cases):
    results = {
        "successful": 0,
        "failed": 0,
        "errors": []
    }
    
    for i, payload in enumerate(test_cases):
        try:
            print(f"Running test {i+1}/{len(test_cases)}")
            print(f"Payload: {payload}")
            
            response = requests.post(base_url, headers=headers, json=payload, timeout=10)
            
            if response.status_code == 200:
                results["successful"] += 1
                print(f"Success! Status code: {response.status_code}")
                # Print summary of response (first level only for readability)
                response_data = response.json()
                summary = {k: (v if not isinstance(v, (dict, list)) else "...") for k, v in response_data.items()}
                print(f"Response summary: {summary}")
            else:
                results["failed"] += 1
                error_info = {
                    "status_code": response.status_code,
                    "payload": payload,
                    "response": response.text[:100] + "..." if len(response.text) > 100 else response.text
                }
                results["errors"].append(error_info)
                print(f"Failed! Status code: {response.status_code}")
                print(f"Error: {error_info['response']}")
            
            # Sleep briefly to avoid overloading the API
            time.sleep(0.5)
            
        except Exception as e:
            results["failed"] += 1
            error_info = {
                "exception": str(e),
                "payload": payload
            }
            results["errors"].append(error_info)
            print(f"Exception: {str(e)}")
            
        print("-" * 50)
    
    return results

# Run the tests and print summary
if __name__ == "__main__":
    print(f"Starting API tests with {len(test_cases)} test cases...")
    start_time = time.time()
    
    results = test_api(test_cases)
    
    elapsed_time = time.time() - start_time
    
    print("\n" + "=" * 50)
    print("TEST SUMMARY")
    print("=" * 50)
    print(f"Total test cases: {len(test_cases)}")
    print(f"Successful requests: {results['successful']}")
    print(f"Failed requests: {results['failed']}")
    print(f"Success rate: {results['successful'] / len(test_cases) * 100:.2f}%")
    print(f"Total time: {elapsed_time:.2f} seconds")
    
    if results["failed"] > 0:
        print("\nERROR DETAILS:")
        for i, error in enumerate(results["errors"]):
            print(f"\nError {i+1}:")
            pprint(error)