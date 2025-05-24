import torch
import torch.nn as nn
import joblib
import numpy as np

class PVSelfConsumptionNet(nn.Module):
    def __init__(self, input_size=4, hidden_sizes=[64, 32, 16], dropout_rate=0.2):
        super(PVSelfConsumptionNet, self).__init__()
        layers = []
        prev_size = input_size
        
        for hidden_size in hidden_sizes:
            layers.extend([
                nn.Linear(prev_size, hidden_size),
                nn.BatchNorm1d(hidden_size),
                nn.ReLU(),
                nn.Dropout(dropout_rate)
            ])
            prev_size = hidden_size
        
        layers.append(nn.Linear(prev_size, 1))
        self.network = nn.Sequential(*layers)
        
    def forward(self, x):
        return self.network(x)

def predict_self_consumption(occupancy_type, annual_consumption, pv_generation, battery_size):
    # Map occupancy type to normalized days (divided by 5 working days)
    occupancy_mapping = {
        'home_all_day': 1.0,      # 5/5
        'in_half_the_day': 0.5,   # 2.5/5
        'out_during_day': 0.0     # 0/5
    }
    
    occupancy_normalized = occupancy_mapping[occupancy_type]
    
    # Load model and scaler
    model = PVSelfConsumptionNet()
    model.load_state_dict(torch.load('final_pv_model.pth', map_location='cpu'))
    model.eval()
    
    scaler = joblib.load('feature_scaler.pkl')
    
    # Prepare and scale features
    features = np.array([[occupancy_normalized, annual_consumption, pv_generation, battery_size]])
    features_scaled = scaler.transform(features)
    
    # Make prediction
    with torch.no_grad():
        features_tensor = torch.FloatTensor(features_scaled)
        prediction = model(features_tensor).item()
    
    return prediction

# Demo example
if __name__ == "__main__":
    print("🔮 PV Self-Consumption Prediction Demo")
    print("="*50)
    
    scenarios = [
        # Description, occupancy_type, annual_consumption, pv_generation, battery_size
        ('Home all day, 4000 kWh consumption, 3000 kWh PV generation, 8 kWh battery', 
         'in_half_the_day', 4000, 3000, 8.0),
        # ('Out during day, 3000 kWh consumption, 4000 kWh PV generation, 5 kWh battery', 
        #  'out_during_day', 3000, 4000, 5.0),
        # ('Half day, 3500 kWh consumption, 2500 kWh PV generation, no battery', 
        #  'in_half_the_day', 3500, 2500, 0.0),
    ]
    
    for desc, occ, cons, pv, batt in scenarios:
        pred = predict_self_consumption(occ, cons, pv, batt)
        print(f"{desc}: {pred:.1f}% self-consumption") 