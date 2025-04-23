import re
import sys

def parse_jmh_output(file_path):
    """Parses JMH output from a file and returns a list of dictionaries containing results for each benchmark."""

    all_results = []

    try:
        with open(file_path, 'r') as f:
            output = f.read()

        # Find all "Result" blocks
        result_blocks = re.finditer(r'Result "(.*?)"\:\n(.*?)(?=\n\n|\Z)', output, re.DOTALL)

        for block_match in result_blocks:
            results = {}
            benchmark_name = block_match.group(1)
            block_content = block_match.group(2)
            results['benchmark'] = benchmark_name

            # Extract score and error
            score_match = re.search(r'([\d.]+) ±\(([\d.]+)%\) ([\d.]+)\s+ms/op', block_content)
            if score_match:
                results['score'] = float(score_match.group(1))
                results['error_percent'] = float(score_match.group(2))
                results['error'] = float(score_match.group(3))
            else:
                score_approx_match = re.search(r'≈\s+(10[⁻-]\d+)\s+ms/op', block_content)
                if score_approx_match:
                    try:
                        results['score'] = float(score_approx_match.group(1).replace('⁻', '-'))
                    except ValueError:
                        results['score_approx'] = score_approx_match.group(1)

            # Extract min, avg, max
            min_avg_max_match = re.search(r'\(min, avg, max\) = \(([\d.]+), ([\d.]+), ([\d.]+)\)', block_content)
            if min_avg_max_match:
                results['min'] = float(min_avg_max_match.group(1))
                results['avg'] = float(min_avg_max_match.group(2))
                results['max'] = float(min_avg_max_match.group(3))

            all_results.append(results)

        return all_results

    except FileNotFoundError:
        print(f"Error: File not found: {file_path}")
        return None
    except Exception as e:
        print(f"An error occurred: {e}")
        return None

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python parse_jmh.py <jmh_output_file>")
    else:
        file_path = sys.argv[1]
        parsed_results = parse_jmh_output(file_path)
        if parsed_results:
            print(parsed_results)