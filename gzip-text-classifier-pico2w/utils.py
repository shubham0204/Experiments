import json

def samples_to_json(comments, labels, filename):
    data = {
        "comments": [],
        "labels": []
    }
    for c, l in zip(comments, labels):
        data["comments"].append(c)
        data["labels"].append(l)
    with open(filename, "w") as f:
        json.dump(data, f)

def samples_from_json(filename):
    with open(filename) as f:
        data = json.load(f)
    return data["comments"], data["labels"]

def write_as_c_string_array(filename, dst_filename):
    with open(filename, "r") as f:
        data = json.load(f)
    with open("templates/data.h", "r") as template:
        template = template.read()

    samples = ""
    for s in data["comments"]:
        s = s.replace('"', '')
        samples += f'\"{s}\",\n'

    labels = ""
    for l in data["labels"]:
        labels += f'{l},\n'

    template = template.replace("<sampleCount>", str(len(data["comments"])))
    template = template.replace("<samples>", samples)
    template = template.replace("<labels>", labels)

    with open(dst_filename, "w") as f:
        f.write(template)